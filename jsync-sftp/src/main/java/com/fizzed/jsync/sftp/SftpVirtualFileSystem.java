package com.fizzed.jsync.sftp;

import com.fizzed.jsync.sftp.impl.SftpATTRSAccessor;
import com.fizzed.jsync.sftp.impl.SignalClosingOutputStream;
import com.fizzed.jsync.vfs.*;
import com.fizzed.jsync.vfs.util.Checksums;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static java.util.Arrays.asList;

public class SftpVirtualFileSystem extends AbstractVirtualFileSystem {
    static private final Logger log = LoggerFactory.getLogger(SftpVirtualFileSystem.class);

    private final Session ssh;
    private final boolean closeSsh;
    private final ChannelSftp sftp;
    private final boolean closeSftp;
    private int maxCommandLength;
    private final boolean windows;

    protected SftpVirtualFileSystem(String name, VirtualPath pwd, Session ssh, boolean closeSsh, ChannelSftp sftp, boolean closeSftp, boolean windows) {
        // everything but windows is case sensitive
        super(name, pwd, !windows);
        this.ssh = ssh;
        this.closeSsh = closeSsh;
        this.sftp = sftp;
        this.closeSftp = closeSftp;
        this.maxCommandLength = 7000;       // windows shell limit is 8,191, linux/mac/bsd is effectively unlimited
        this.windows = windows;
    }

    static public SftpVirtualFileSystem open(String host) throws IOException {
        try {
            // does the host include a port?
            Integer port = null;
            final int colonIndex = host.indexOf(':');
            if (colonIndex > 0) {
                port = Integer.parseInt(host.substring(colonIndex+1));
                host = host.substring(0, colonIndex);
            }

            final JSch jsch = new JSch();

            // user's home ssh config
            final Path sshConfigFile = Paths.get(System.getProperty("user.home"), ".ssh", "config");
            if (Files.exists(sshConfigFile)) {
                ConfigRepository configRepository = OpenSSHConfig.parseFile(sshConfigFile.toString());

                jsch.setConfigRepository(configRepository);
            }

            // load identities from ~/.ssh/id_*
            final Path sshDir = Paths.get(System.getProperty("user.home"), ".ssh");
            if (Files.isDirectory(sshDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(sshDir, "id_*")) {
                    for (Path path : stream) {
                        // skip any ".pub" files though
                        if (path.toString().endsWith(".pub")) {
                            continue;
                        }
                        jsch.addIdentity(path.toString());
                    }
                }
            }

            final Session ssh;
            if (port != null) {
                ssh = jsch.getSession(null, host, port);
            } else {
                ssh = jsch.getSession(host);
            }

            ssh.setDaemonThread(true);
            ssh.setConfig("StrictHostKeyChecking", "no");

            log.info("Connecting ssh to {}...", host);

            ssh.connect();

            return open(ssh, true);
        } catch (JSchException e) {
            throw toIOException(e);
        }
    }

    static public SftpVirtualFileSystem open(Session ssh, boolean closeSsh) throws IOException {
        try {
            final ChannelSftp sftp = (ChannelSftp)ssh.openChannel("sftp");

            log.info("Opening sftp channel to {}...", ssh.getHost());

            sftp.connect();

            return open(ssh, closeSsh, sftp, true);
        } catch (JSchException e) {
            throw toIOException(e);
        }
    }

    static public SftpVirtualFileSystem open(Session ssh, boolean closeSsh, ChannelSftp sftp, boolean closeSftp) throws IOException {
        final String name = ssh.getHost();

        log.debug("Opening filesystem {}...", name);

        final String pwdRaw;
        try {
            pwdRaw = sftp.pwd();
        } catch (SftpException e) {
            throw toIOException(e);
        }

        final VirtualPath pwd = VirtualPath.parse(pwdRaw, true);

        log.debug("Detected pwd {}", pwd);

        boolean windows = false;

        // this is likely a "windows" system if the 2nd char is :
        if (pwdRaw.length() > 2 && pwdRaw.charAt(2) == ':') {
            // TODO: should we confirm by running a command that exists only windows to confirm?
            // for now we'll just assume it is
            windows = true;
            log.debug("Detected windows-based sftp server");
        }

        return new SftpVirtualFileSystem(name, pwd, ssh, closeSsh, sftp, closeSftp, windows);
    }

    @Override
    public void close() throws Exception {
        if (this.closeSftp) {
            try {
                this.sftp.disconnect();
            } catch (Exception e) {
                // ignore
            }
        }
        if (this.closeSsh) {
            try {
                this.ssh.disconnect();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public StatKind getStatKind() {
        // for now, we'll claim full POSIX as the sftp server itself does the POSIX translation
        return StatKind.POSIX;
    }

    @Override
    public boolean isChecksumSupported(Checksum checksum) throws IOException {
        if (this.windows) {
            switch (checksum) {
                case MD5:
                case SHA1:
                    return true;
                default:
                    return false;
            }
        }

        // otherwise, we are on posix and we can actually check whether these would work or not
        switch (checksum) {
            case CK: {
                int exitValue = this.exec(this.ssh, "which cksum", null, null, null);
                return exitValue == 0;
            }
            case MD5: {
                int exitValue = this.exec(this.ssh, "which md5sum", null, null, null);
                return exitValue == 0;
            }
            case SHA1: {
                int exitValue = this.exec(this.ssh, "which sha1sum", null, null, null);
                return exitValue == 0;
            }
            default:
                return false;
        }
    }

    @Override
    protected List<Checksum> doDetectChecksums() throws IOException {
        // windows is easy, return what powershell supports
        if (this.windows) {
            return asList(Checksum.MD5, Checksum.SHA1);
        }

        // otherwise, we are on posix and we can actually check whether these would work or not
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // check if anything is supported
        int exitValue = this.exec(this.ssh, "which cksum md5sum sha1sum", null, baos, null);
        if (exitValue != 0) {
            return Collections.emptyList();
        }

        final String output = baos.toString(StandardCharsets.UTF_8.name());
        final List<Checksum> checksums = new ArrayList<>();
        if (output.contains("cksum")) {
            checksums.add(Checksum.CK);
        }
        if (output.contains("md5sum")) {
            checksums.add(Checksum.MD5);
        }
        if (output.contains("sha1sum")) {
            checksums.add(Checksum.SHA1);
        }

        return checksums;
    }

    public int getMaxCommandLength() {
        return maxCommandLength;
    }

    public SftpVirtualFileSystem setMaxCommandLength(int maxCommandLength) {
        this.maxCommandLength = maxCommandLength;
        return this;
    }

    protected VirtualPath withStats(VirtualPath path, SftpATTRS attrs) throws IOException {
        final long size = attrs.getSize();
        final long modifiedTime = attrs.getMTime() * 1000L;
        final long accessedTime = attrs.getATime() * 1000L;
        // sftp stuffs extra stuff like the file type in the permissions value, we don't care about it
        final int perms = attrs.getPermissions() & 07777;

        final VirtualFileType type;
        if (attrs.isDir()) {
            type = VirtualFileType.DIR;
        } else if (attrs.isReg()) {
            type = VirtualFileType.FILE;
        } else if (attrs.isLink()) {
            type = VirtualFileType.SYMLINK;
        } else {
            type = VirtualFileType.OTHER;
        }

        final VirtualFileStat stat = new VirtualFileStat(type, size, modifiedTime, accessedTime, perms);

        return new VirtualPath(path.getParentPath(), path.getName(), type == VirtualFileType.DIR, stat);
    }

    @Override
    public VirtualPath stat(VirtualPath path) throws IOException {
        try {
            final SftpATTRS attrs = this.sftp.lstat(path.toString());

            return this.withStats(path, attrs);
        } catch (SftpException e) {
            throw toIOException(e);
        }
    }

    @Override
    public void updateStat(VirtualPath path, VirtualFileStat stat) throws IOException {
        try {
            final SftpATTRS attrs = SftpATTRSAccessor.createSftpATTRS();

            // TODO: are we updating uid/gid?
            Integer uid = null;
            Integer gid = null;

            // TODO: are we updating permissions?
//            Integer perms = null;
            int perms = stat.getPermissions();
            attrs.setPERMISSIONS(perms);

            // are we updating mtime/atime?d
            int mtime = (int)(stat.getModifiedTime()/1000);
            int atime = (int)(stat.getAccessedTime()/1000);
            attrs.setACMODTIME(atime, mtime);

            this.sftp.setStat(path.toString(), attrs);
        } catch (SftpException e) {
            throw toIOException(e);
        }
    }

    @Override
    public List<VirtualPath> ls(VirtualPath path) throws IOException {
        final Vector<ChannelSftp.LsEntry> entries;
        try {
            entries = this.sftp.ls(path.toString());
        } catch (SftpException e) {
            throw toIOException(e);
        }

        final List<VirtualPath> childPaths = new ArrayList<>();

        for (ChannelSftp.LsEntry entry : entries) {
            if (entry.getFilename().equals(".") || entry.getFilename().equals("..")) {
                continue;   // skip these
            }

            // dir true/false doesn't matter, stats call next will correct it
            VirtualPath childPathWithoutStats = path.resolve(entry.getFilename(), false);
            VirtualPath childPath = this.withStats(childPathWithoutStats, entry.getAttrs());
            childPaths.add(childPath);
        }

        return childPaths;
    }

    @Override
    public void mkdir(VirtualPath path) throws IOException {
        try {
            this.sftp.mkdir(path.toString());
        } catch (SftpException e) {
            throw toIOException(e);
        }
    }

    @Override
    public void rm(VirtualPath path) throws IOException {
        try {
            this.sftp.rm(path.toString());
        } catch (SftpException e) {
            throw toIOException(e);
        }
    }

    @Override
    public void rmdir(VirtualPath path) throws IOException {
        try {
            this.sftp.rmdir(path.toString());
        } catch (SftpException e) {
            throw toIOException(e);
        }
    }

    @Override
    public InputStream readFile(VirtualPath path) throws IOException {
        try {
            return this.sftp.get(path.toString(), null, 0L);
        } catch (SftpException e) {
            throw toIOException(e);
        }
    }

    @Override
    public void writeFile(InputStream input, VirtualPath path) throws IOException {
        try {
            this.sftp.put(input, path.toString(), ChannelSftp.OVERWRITE);
        } catch (SftpException e) {
            throw toIOException(e);
        }
    }

    @Override
    public OutputStream writeStream(VirtualPath path) throws IOException {
        try {
            return this.sftp.put(path.toString(), ChannelSftp.OVERWRITE);
        } catch (SftpException e) {
            throw toIOException(e);
        }
    }

    @Override
    public void cksums(List<VirtualPath> paths) throws IOException {
       /* if (this.windows) {
            throw new UnsupportedChecksumException("Checksum CK is not supported on windows", null);
        } else {*/
            this.hashFilesOnPosix(Checksum.CK, paths);
//        }
    }

    @Override
    public void md5sums(List<VirtualPath> paths) throws IOException {
        if (this.windows) {
            this.hashFilesOnWindows(Checksum.MD5, paths);
        } else {
            this.hashFilesOnPosix(Checksum.MD5, paths);
        }
    }

    @Override
    public void sha1sums(List<VirtualPath> paths) throws IOException {
        if (this.windows) {
            this.hashFilesOnWindows(Checksum.SHA1, paths);
        } else {
            this.hashFilesOnPosix(Checksum.SHA1, paths);
        }
    }

    // helpers

    static protected IOException toIOException(JSchException e) {
        return new IOException(e.getMessage(), e);
    }

    static protected IOException toIOException(SftpException e) {
        if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
            return new NoSuchFileException(e.getMessage());
        }
        return new IOException(e.getMessage(), e);
    }

    protected int exec(Session ssh, List<String> commands, InputStream input, OutputStream output, OutputStream error) throws IOException {
        // convert List<String> into a single String
        final StringBuilder commandBuilder = new StringBuilder();
        for (String command : commands) {
            String v = command;
            if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                // do nothing, it's already quoted'
            } else if (v.contains(" ") || v.contains("\"") || v.contains("$")) {
                v = "'" + command + "'";
            }
            if (commandBuilder.length() > 0) {
                commandBuilder.append(" ");
            }
            commandBuilder.append(v);
        }

        return this.exec(ssh, commandBuilder.toString(), input, output, error);
    }

    protected int exec(Session ssh, String command, InputStream input, OutputStream output, OutputStream error) throws IOException {
        ChannelExec exec = null;
        try {
            exec = (ChannelExec) ssh.openChannel("exec");

            //log.debug("Executing command: {}", command);

            exec.setCommand(command);

            final CountDownLatch streamClosedLatch = new CountDownLatch(1);

            // Pass the streams to JSch.
            // The 'true' argument tells JSch to close the streams when the channel disconnects,
            // which keeps your resource management cleaner.
            if (input != null) {
                exec.setInputStream(input, false);
            }
            if (output != null) {
                exec.setOutputStream(new SignalClosingOutputStream(output, streamClosedLatch), false);
            }
            if (error != null) {
                exec.setErrStream(new SignalClosingOutputStream(error, streamClosedLatch), false);
            }

            exec.connect();

            // If both outputs are null, the latch will never countdown.
            // In that case, we shouldn't use the latch logic (or we rely on the backup loop).
            boolean usingLatch = (output != null || error != null);

            // 3. EFFICIENT WAIT:
            // Instead of sleeping in a loop immediately, we block here until
            // the output stream sends us the "I'm closed" signal.
            if (usingLatch) {
                try {
                    streamClosedLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for stream closure", e);
                }
            }

            // 4. FINAL SAFETY CHECK:
            // Even after the stream closes, there can be a tiny race condition where
            // the channel status isn't updated for a few milliseconds.
            // This loop will usually run 0 or 1 times now.
            while (!exec.isClosed()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for channel close", e);
                }
            }

            // Now that the channel is closed, this will return the actual exit code
            return exec.getExitStatus();

        } catch (JSchException e) {
            throw new IOException(e);
        } finally {
            // Always disconnect the channel to free up the slot in the SSH session
            if (exec != null) {
                exec.disconnect();
            }
        }
    }

    protected void hashFilesOnPosix(Checksum checksum, List<VirtualPath> paths) throws IOException {
        // name of the executable we will run
        final String exeName;
        switch (checksum) {
            case CK:
                exeName = "cksum";
                break;
            case MD5:
                exeName = "md5sum";
                break;
            case SHA1:
                exeName = "sha1sum";
                break;
            default:
                throw new UnsupportedChecksumException("Unsupported checksum '" + checksum + "' on posix is not supported", null);
        }

        // we need to be smart about how many files we request in bulk, as the command line can only be so long
        final Map<String,VirtualPath> fileMappings = new HashMap<>();
        final List<String> commands = new ArrayList<>();
        int commandLength = 0;

        for (int i = 0; i < paths.size(); i++) {
            final VirtualPath path = paths.get(i);

            // create new or add to request?
            if (commands.isEmpty()) {
                commands.add(exeName);
            }

            // always add full path
            String fullPath = path.toString();

            // native windows paths
//            fullPath = fullPath.substring(1).replace('/', '\\');   // chop off leading '/', and swap / with \

            commands.add(fullPath);
            fileMappings.put(fullPath, path);
            commandLength += fullPath.length();

            // should we send this request?
            if (commandLength >= this.maxCommandLength || (i == paths.size() - 1)) {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final int exitValue = this.exec(this.ssh, commands, null, baos, System.err);

                if (exitValue != 0) {
                    throw new UnsupportedChecksumException("Checksum algorithm " + checksum + " failed virtual filesystem " + this.getName(), null);
                }

                final String output = baos.toString(StandardCharsets.UTF_8.name());

                // parse output into entries
                final List<Checksums.HashEntry> entries;
                switch (checksum) {
                    case CK:
                        entries = Checksums.parsePosixCksumOutput(output);
                        break;
                    case MD5:
                    case SHA1:
                        entries = Checksums.parsePosixHashOutput(output);
                        break;
                    default:
                        throw new UnsupportedChecksumException("Unsupported checksum '" + checksum + "' on posix is not supported", null);
                }

                /*log.error("fileMappings: {}", fileMappings);
                log.error("entries: {}", entries);*/

                for (Checksums.HashEntry entry : entries) {
                    final VirtualPath entryPath = fileMappings.get(entry.getFile());

                    if (entryPath == null) {
                        //log.error("Something may be wrong parsing this output: {}", output);
                        throw new IllegalStateException("Unable to associate hash result with virtual path for '" + entry.getFile() + "'");
                    }

                    if (checksum == Checksum.CK) {
                        entryPath.getStat().setCksum(entry.getCksum());
                    } else if (checksum == Checksum.MD5) {
                        entryPath.getStat().setMd5(entry.getHash());
                    } else if (checksum == Checksum.SHA1) {
                        entryPath.getStat().setSha1(entry.getHash());
                    } else {
                        throw new UnsupportedChecksumException("Unsupported checksum '" + checksum + "' on posix is not supported", null);
                    }
                }

                // reset everything for next run
                commands.clear();
                commandLength = 0;
                fileMappings.clear();
            }
        }
    }

    protected void hashFilesOnWindows(Checksum checksum, List<VirtualPath> paths) throws IOException {
        // we need to be smart about how many files we request in bulk, as the command line can only be so long
        final Map<String,VirtualPath> fileMapping = new HashMap<>();
        final StringBuilder fileListBuilder = new StringBuilder();

        for (int i = 0; i < paths.size(); i++) {
            final VirtualPath path = paths.get(i);

            // convert full path into a valid Windows path?
            // the powershell Get-FileHash will ALWAYS return the native windows path, not any special version we feed in
            final String fullPath = path.toString().substring(1).replace('/', '\\');   // chop off leading '/', and swap / with \
            fileMapping.put(fullPath, path);
            if (fileListBuilder.length() > 0) {
                fileListBuilder.append(",");
            }
            fileListBuilder.append("'").append(fullPath).append("'");

            // should we send this request?
            if (fileListBuilder.length() >= this.maxCommandLength || (i == paths.size() - 1)) {
                // powershell -Command "Get-FileHash -Algorithm MD5 'C:\Path\To\File1.iso', 'D:\Data\File2.zip' | Select-Object Hash, Path | Format-List"
                StringBuilder commandBuilder = new StringBuilder();
                commandBuilder.append("powershell -Command \"Get-FileHash -Algorithm ").append(checksum.toString()).append(" ");
                commandBuilder.append(fileListBuilder);
                commandBuilder.append(" | Select-Object Hash, Path | Format-List\"");

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int exitValue = this.exec(this.ssh, commandBuilder.toString(), null, baos, System.err);

                if (exitValue != 0) {
                    throw new UnsupportedChecksumException("Checksum strategy '" + checksum + "' on windows failed", null);
                }

                final String output = baos.toString(StandardCharsets.UTF_8.name());

                // parse output into entries
                final List<Checksums.HashEntry> entries = Checksums.parsePowershellHashFileOutput(output);
                for (Checksums.HashEntry entry : entries) {
                    final VirtualPath entryPath = fileMapping.get(entry.getFile());

                    if (entryPath == null) {
                        throw new IllegalStateException("Unable to associate hash result with virtual path for '" + entry.getFile() + "'");
                    }

                    if (checksum == Checksum.MD5) {
                        entryPath.getStat().setMd5(entry.getHash());
                    } else if (checksum == Checksum.SHA1) {
                        entryPath.getStat().setSha1(entry.getHash());
                    } else {
                        throw new UnsupportedChecksumException("Checksum '" + checksum + "' on windows is not supported", null);
                    }
                }

                // reset everything for next run
                fileListBuilder.setLength(0);
                fileMapping.clear();
            }
        }
    }

}