package com.fizzed.jsync.sftp;

import com.fizzed.jsync.vfs.VirtualFileSystem;
import com.fizzed.jsync.vfs.VirtualVolume;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;

import java.io.IOException;

public class SftpVirtualVolume implements VirtualVolume {

    private final String host;
    private final Session ssh;
    private final boolean closeSsh;
    private final ChannelSftp sftp;
    private final boolean closeSftp;
    private final String path;

    public SftpVirtualVolume(String host, Session ssh, boolean closeSsh, ChannelSftp sftp, boolean closeSftp, String path) {
        this.host = host;
        this.ssh = ssh;
        this.closeSsh = closeSsh;
        this.sftp = sftp;
        this.closeSftp = closeSftp;
        this.path = path;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public VirtualFileSystem openFileSystem() throws IOException {
        if (this.ssh != null && this.sftp != null) {
            return SftpVirtualFileSystem.open(this.ssh, this.closeSsh, this.sftp, this.closeSftp);
        } else if (this.ssh != null) {
            return SftpVirtualFileSystem.open(this.ssh, this.closeSsh);
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
        return new SftpVirtualVolume(ssh, null, true, null, true, path);
    }

    static public SftpVirtualVolume sftpVolume(Session ssh, boolean closeSsh, String path) {
        return new SftpVirtualVolume(null, ssh, closeSsh, null, true, path);
    }

    static public SftpVirtualVolume sftpVolume(Session ssh, boolean closeSsh, ChannelSftp sftp, boolean closeSftp, String path) {
        return new SftpVirtualVolume(null, ssh, closeSsh, sftp, closeSftp, path);
    }

}