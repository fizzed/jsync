package com.fizzed.jsync.vfs;

import com.fizzed.jsync.vfs.util.Checksums;
import com.fizzed.jsync.vfs.util.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class LocalVirtualFileSystem extends AbstractVirtualFileSystem {
    static private final Logger log = LoggerFactory.getLogger(LocalVirtualFileSystem.class);

    private final boolean posix;

    public LocalVirtualFileSystem(String name, VirtualPath pwd, boolean caseSensitive, boolean posix) {
        super(name, pwd, caseSensitive);
        this.posix = posix;
    }

    static public LocalVirtualFileSystem open() {
        return open(Paths.get("."));
    }

    static public LocalVirtualFileSystem open(Path workingDir) {
        final String name = "<local>";

        log.info("Opening filesystem {}...", name);

        // current working directory is our "pwd"
        final Path currentWorkingDir = workingDir.toAbsolutePath().normalize();

        final VirtualPath pwd = VirtualPath.parse(currentWorkingDir.toString(), true);

        final boolean isPosixAttributes = FileSystems.getDefault()
            .supportedFileAttributeViews()
            .contains("posix");

        log.debug("Detected filesystem {} has pwd={}, posix={}", name, pwd, isPosixAttributes);

        // everything is case-sensitive except windows
        final boolean caseSensitive = !System.getProperty("os.name").toLowerCase().contains("windows");

        log.debug("Detected filesystem {} is case-sensitive={}", name, caseSensitive);

        return new LocalVirtualFileSystem(name, pwd, caseSensitive, isPosixAttributes);
    }

    @Override
    public void close() throws Exception {
        // nothing to do
    }

    public boolean isPosix() {
        return this.posix;
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public StatModel getStatModel() {
        if (this.posix) {
            return StatModel.POSIX;
        } else {
            return StatModel.BASIC;
        }
    }

    @Override
    protected List<Checksum> doDetectChecksums() throws IOException {
        // everything is supported
        return asList(Checksum.CK, Checksum.MD5, Checksum.SHA1);
    }

    protected Path toNativePath(VirtualPath path) {
        return Paths.get(path.toString());
    }

    protected VirtualPath withStat(VirtualPath path) throws IOException {
        final Path nativePath = this.toNativePath(path);

        // Fetch all attributes in ONE operation (and don't follow symlinks, we need to know the type)
        final PosixFileAttributes posixAttrs;
        final BasicFileAttributes attrs;
        if (this.posix) {
            posixAttrs = Files.readAttributes(nativePath, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            attrs = posixAttrs;
        } else {
            posixAttrs = null;
            attrs = Files.readAttributes(nativePath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        }

        // basic attributes get us much of what we need
        final long size = attrs.size();
        final long modifiedTime = attrs.lastModifiedTime().toMillis();
        final long accessedTime = attrs.lastAccessTime().toMillis();

        // map file type
        final VirtualFileType type;
        if (attrs.isDirectory()) {
            type = VirtualFileType.DIR;
        } else if (attrs.isRegularFile()) {
            type = VirtualFileType.FILE;
        } else if (attrs.isSymbolicLink()) {
            type = VirtualFileType.SYMLINK;
        } else if (attrs.isOther()) {
            type = VirtualFileType.OTHER;
        } else {
            // should be all the types, but just in case
            log.warn("Unknown file type mapping for path: {} (defaulting to other)", path);
            type = VirtualFileType.OTHER;
        }

        // permissions are a tad trickier if they aren't really supported
        final int perms;
        if (posixAttrs != null) {
            perms = Permissions.toPosixInt(posixAttrs.permissions());
        } else {
            // use basic permissions, usually ends up being 700 from what I can gather
            final Set<PosixFilePermission> simulatedPosixPermissions = Permissions.toBasicPermissions(nativePath);
            perms = Permissions.toPosixInt(simulatedPosixPermissions);
        }

        final VirtualFileStat stat = new VirtualFileStat(type, size, modifiedTime, accessedTime, perms);

        return new VirtualPath(path.getParentPath(), path.getName(), type == VirtualFileType.DIR, stat);
    }



    @Override
    public VirtualPath stat(VirtualPath path) throws IOException {
        return this.withStat(path);
    }

    @Override
    public void updateStat(VirtualPath path, VirtualFileStat stat, Collection<StatUpdateOption> options) throws IOException {
        final Path nativePath = this.toNativePath(path);

        final PosixFileAttributeView posixView;
        final BasicFileAttributeView view;
        if (this.posix) {
            posixView = Files.getFileAttributeView(nativePath, PosixFileAttributeView.class);
            view = posixView;
        } else {
            posixView = null;
            view = Files.getFileAttributeView(nativePath, BasicFileAttributeView.class);
        }

        if (options.contains(StatUpdateOption.PERMISSIONS)) {
            final Set<PosixFilePermission> posixFilePermissions = Permissions.toPosixFilePermissions(stat.getPermissions());
            if (posixView != null) {
                posixView.setPermissions(posixFilePermissions);
            } else {
                Permissions.setBasicPermissions(nativePath, posixFilePermissions);
            }
        }

        if (options.contains(StatUpdateOption.TIMESTAMPS)) {
            // 2. Prepare the times
            FileTime newModifiedTime = FileTime.fromMillis(stat.getModifiedTime());
            FileTime newAccessedTime = FileTime.fromMillis(stat.getAccessedTime());

            // 3. Update all three in ONE operation
            // Signature: setTimes(lastModified, lastAccess, createTime)
            // Pass 'null' if you want to leave a specific timestamp unchanged.
            view.setTimes(newModifiedTime, newAccessedTime, null);
        }
    }

    @Override
    public List<VirtualPath> ls(VirtualPath path) throws IOException {
        final Path nativePath = this.toNativePath(path);
        if (Files.isDirectory(nativePath)) {
            // TODO: apparently walkFileTree is way faster on windows as it includes BasicFileAttributes, negating the need for a 2nd kernel system call
            List<VirtualPath> childPaths = new ArrayList<>();
            try (Stream<Path> files = Files.list(nativePath)) {
                for (Iterator<Path> it = files.iterator(); it.hasNext(); ) {
                    Path nativeChildPath = it.next();

                    // dir true/false doesn't matter, stats call next will correct it
                    VirtualPath childPathWithoutStats = path.resolve(nativeChildPath.getFileName().toString(), false);

                    VirtualPath childPath = this.withStat(childPathWithoutStats);
                    childPaths.add(childPath);
                }
            }
            return childPaths;
        } else {
            throw new IOException("Not a directory: " + path);
        }
    }

    @Override
    public void mkdir(VirtualPath path) throws IOException {
        final Path nativePath = this.toNativePath(path);
        // to mirror what sftp provides, this should NOT make parents automatically
        Files.createDirectory(nativePath);
    }

    @Override
    public void rm(VirtualPath path) throws IOException {
        final Path nativePath = this.toNativePath(path);
        Files.delete(nativePath);
    }

    @Override
    public void rmdir(VirtualPath path) throws IOException {
        final Path nativePath = this.toNativePath(path);
        // to mirror what sftp provides, this should NOT make parents automatically
        Files.delete(nativePath);
    }

    @Override
    public InputStream readFile(VirtualPath path) throws IOException {
        final Path nativePath = this.toNativePath(path);
        return Files.newInputStream(nativePath);
    }

    @Override
    public void writeFile(InputStream input, VirtualPath path) throws IOException {
        final Path nativePath = this.toNativePath(path);
        // its important we allow replacing existing files
        Files.copy(input, nativePath, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public OutputStream writeStream(VirtualPath path) throws IOException {
        final Path nativePath = this.toNativePath(path);
        // it's important we allow replacing existing files
        return Files.newOutputStream(nativePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Override
    public void cksums(List<VirtualPath> paths) throws IOException {
        for (VirtualPath path : paths) {
            try (InputStream input = this.readFile(path)) {
                long cksum = Checksums.cksum(input);
                path.getStat().setCksum(cksum);
            }
        }
    }

    @Override
    public void md5sums(List<VirtualPath> paths) throws IOException {
        this.hashFiles("MD5", paths);
    }

    @Override
    public void sha1sums(List<VirtualPath> paths) throws IOException {
        this.hashFiles("SHA1", paths);
    }

    protected void hashFiles(String algorithm, List<VirtualPath> paths) throws IOException {
        for (VirtualPath path : paths) {
            try (InputStream input = this.readFile(path)) {
                String digest = Checksums.hash(algorithm, input);
                if ("MD5".equals(algorithm)) {
                    path.getStat().setMd5(digest);
                } else if ("SHA1".equals(algorithm)) {
                    path.getStat().setSha1(digest);
                }
            }
        }
    }

}
