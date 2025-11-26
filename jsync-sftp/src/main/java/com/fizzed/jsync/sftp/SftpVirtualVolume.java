package com.fizzed.jsync.sftp;

import com.fizzed.jsync.vfs.VirtualFileSystem;
import com.fizzed.jsync.vfs.VirtualVolume;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;

import java.io.IOException;

public class SftpVirtualVolume implements VirtualVolume {

    private final String host;
    private final Session ssh;
    private final ChannelSftp sftp;
    private final String path;

    public SftpVirtualVolume(String host, Session ssh, ChannelSftp sftp, String path) {
        this.host = host;
        this.ssh = ssh;
        this.sftp = sftp;
        this.path = path;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public VirtualFileSystem openFileSystem() throws IOException {
        if (this.ssh != null && this.sftp != null) {
            return SftpVirtualFileSystem.open(this.ssh, this.sftp);
        } else if (this.ssh != null) {
            return SftpVirtualFileSystem.open(this.ssh);
        } else {
            return SftpVirtualFileSystem.open(this.host);
        }
    }

    @Override
    public String toString() {
        if (this.ssh != null) {
            return this.ssh.getHost() + ":" + this.path;
        } else {
            return this.host + ":" + this.path;
        }
    }

    static public SftpVirtualVolume sftpVolume(String ssh, String path) {
        return new SftpVirtualVolume(ssh, null, null, path);
    }

    static public SftpVirtualVolume sftpVolume(Session ssh, String path) {
        return new SftpVirtualVolume(null, ssh, null, path);
    }

    static public SftpVirtualVolume sftpVolume(Session ssh, ChannelSftp sftp, String path) {
        return new SftpVirtualVolume(null, ssh, sftp, path);
    }

}