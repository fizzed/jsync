package com.fizzed.jsync.sftp;

import com.fizzed.jsync.vfs.VirtualPath;
import com.fizzed.jsync.vfs.VirtualVolume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

import static com.fizzed.jsync.sftp.SftpVirtualVolume.sftpVolume;
import static com.fizzed.jsync.vfs.LocalVirtualVolume.localVolume;

public class SftpVirtualFileSystemDemo {
    static private final Logger log = LoggerFactory.getLogger(SftpVirtualFileSystemDemo.class);

    static public void main(String[] args) throws Exception {

//        final SftpVirtualFileSystem vfs = SftpVirtualFileSystem.open("bmh-dev-x64-indy25-1");
        final SftpVirtualFileSystem vfs = SftpVirtualFileSystem.open("bmh-build-x64-win11-1");

//        final VirtualPath stat = vfs.stat(VirtualPath.parse(".ssh/authorized_keys"));
        final VirtualPath stat = vfs.stat(VirtualPath.parse("remote-build"));

        log.info("file: {}, perms={}", stat, stat.getStat().getPermissionsOctal());
    }

}