package com.fizzed.jsync.sftp;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * A lightweight, embedded SSH/SFTP server for testing purposes.
 * <p>
 * This server is configured to be Permissive:
 * 1. Accepts ANY username/password.
 * 2. Accepts ANY public key.
 * 3. "Jails" ALL users to the specified local directory (Virtual File System).
 */
public class TestSshServer implements AutoCloseable {

    private SshServer sshd;
    private final int port;

    /**
     * Creates a server on a random available port.
     */
    public TestSshServer() {
        this(0);
    }

    public TestSshServer(int port) {
        this.port = port;
    }

    /**
     * Starts the SSH server serving the specified local directory as the root.
     *
     * @param rootDirectory The local directory to serve (e.g., Paths.get("src/test/resources"))
     */
    public void start(Path rootDirectory) throws IOException {
        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(port);

        // 1. Generate a generic host key on the fly (avoids needing key files)
        // This creates a 'hostkey.ser' file in the running directory if missing.
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get("target/hostkey.ser")));

        // 2. DISABLE AUTHENTICATION CHECKS (Permissive Mode)
        // Accepts any password
        sshd.setPasswordAuthenticator((user, pass, session) -> true);
        // Accepts any public key
        sshd.setPublickeyAuthenticator((user, key, session) -> true);

        // 3. Setup SFTP Subsystem
        sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));

        // 4. Virtual Filesystem (Jails ANY user to rootDirectory)
        // We override the factory to ignore the username and always serve the rootDirectory.
        VirtualFileSystemFactory fileSystemFactory = new VirtualFileSystemFactory() {
            @Override
            public Path getUserHomeDir(org.apache.sshd.common.session.SessionContext session) {
                return rootDirectory;
            }
        };
        sshd.setFileSystemFactory(fileSystemFactory);

        // 5. Setup Command Support
        // This uses the host OS commands.
        // Note: exec() commands run on the HOST OS and are NOT JAILED by the VirtualFileSystem.
        sshd.setCommandFactory((channel, command) -> new CustomJavaCommand(command));

        sshd.start();
    }

    public int getPort() {
        return sshd.getPort();
    }

    @Override
    public void close() throws IOException {
        if (sshd != null) {
            sshd.stop();
        }
    }

    static private class CustomJavaCommand implements Command {
        private final String commandString;
        private InputStream in;
        private OutputStream out;
        private OutputStream err;
        private ExitCallback exitCallback;

        public CustomJavaCommand(String commandString) {
            this.commandString = commandString;
        }

        @Override
        public void start(ChannelSession channel, Environment env) throws IOException {
            // We run the logic in a separate thread so we don't block the SSH NIO thread
            new Thread(() -> {
                try {
                    this.handleCommand();
                    if (exitCallback != null) {
                        exitCallback.onExit(0); // Success exit code
                    }
                } catch (Exception e) {
                    try {
                        String errorMsg = "Error executing command: " + e.getMessage() + "\n";
                        if (err != null) err.write(errorMsg.getBytes(StandardCharsets.UTF_8));
                        if (exitCallback != null) exitCallback.onExit(1, e.getMessage());
                    } catch (IOException ignored) {}
                }
            }).start();
        }

        private void handleCommand() throws IOException {
            // MOCK LOGIC: Decide what to return based on the command string
            String response = "";

            if (commandString.trim().equals("which md5sum")) {
                response = "/usr/bin/md5sum";
            } else {
                throw new IOException("Unknown command: " + commandString);
            }

            if (out != null) {
                out.write(response.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        }

        @Override
        public void destroy(ChannelSession channel) {}

        @Override
        public void setInputStream(InputStream in) { this.in = in; }

        @Override
        public void setOutputStream(OutputStream out) { this.out = out; }

        @Override
        public void setErrorStream(OutputStream err) { this.err = err; }

        @Override
        public void setExitCallback(ExitCallback callback) { this.exitCallback = callback; }
    }

}