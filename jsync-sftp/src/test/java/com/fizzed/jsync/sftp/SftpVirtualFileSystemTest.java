package com.fizzed.jsync.sftp;

import com.fizzed.crux.util.MoreFiles;
import com.fizzed.crux.util.Resources;
import com.fizzed.jsync.vfs.VirtualFileType;
import com.fizzed.jsync.vfs.VirtualPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SftpVirtualFileSystemTest {
    static private final Logger log = LoggerFactory.getLogger(SftpVirtualFileSystemTest.class);

    static private TestSshServer sshServer;
    static private Path sftpRootDir;
    static private SftpVirtualFileSystem defaultVfs;

    @BeforeAll
    static public void setup() throws Exception {
        final Path projectDir = Resources.file("/locator.txt").resolve("../..").toAbsolutePath().normalize();
        sftpRootDir = projectDir.resolve("target/sftp-root");
        Files.createDirectories(sftpRootDir);
        sshServer = new TestSshServer();
        sshServer.start(sftpRootDir);
        defaultVfs = SftpVirtualFileSystem.open("localhost:" + sshServer.getPort());
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        MoreFiles.deleteDirectoryIfExists(sftpRootDir);
        Files.createDirectories(sftpRootDir);

        // create some default files
        Path rootFile = sftpRootDir.resolve("root.txt");
        Files.write(rootFile, "hello".getBytes());
        Path dirA = sftpRootDir.resolve("a");
        Files.createDirectories(dirA);
        Path dirB = sftpRootDir.resolve("b");
        Files.createDirectories(dirB);
        Path dirBFile = dirB.resolve("b.txt");
        Files.write(dirBFile, "hello".getBytes());
    }

    @AfterAll
    static public void teardown() throws Exception {
        try {
            defaultVfs.close();
        } catch (Exception e) {
            // ignore
        }
        try {
            sshServer.close();
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    public void pwd() throws Exception {
        VirtualPath vp = defaultVfs.pwd();

        assertThat(vp.getParentPath()).isEqualTo("");
        assertThat(vp.getName()).isEqualTo("");
        assertThat(vp.toFullPath()).isEqualTo("/");
    }

    @Test
    public void getName() throws Exception {
        assertThat(defaultVfs.getName()).isEqualTo("localhost");
    }

    @Test
    public void isRemote() throws Exception {
        assertThat(defaultVfs.isRemote()).isTrue();
    }

    @Test
    public void ls() throws Exception {
        final List<VirtualPath> list = defaultVfs.ls(VirtualPath.parse("/"));
        list.sort(Comparator.comparing(VirtualPath::toFullPath));

        assertThat(list).hasSize(3);
        assertThat(list.get(0).getName()).isEqualTo("a");
        assertThat(list.get(0).toFullPath()).isEqualTo("/a");
        assertThat(list.get(0).isDirectory()).isTrue();
        assertThat(list.get(1).getName()).isEqualTo("b");
        assertThat(list.get(1).toFullPath()).isEqualTo("/b");
        assertThat(list.get(2).getName()).isEqualTo("root.txt");
        assertThat(list.get(2).toFullPath()).isEqualTo("/root.txt");
        assertThat(list.get(2).isDirectory()).isFalse();
        assertThat(list.get(2).getStat().getType()).isEqualTo(VirtualFileType.FILE);
        assertThat(list.get(2).getStat().getSize()).isEqualTo(Files.size(sftpRootDir.resolve("root.txt")));
        assertThat(list.get(2).getStat().getModifiedTime()).isCloseTo(Files.getLastModifiedTime(sftpRootDir.resolve("root.txt")).toMillis(), within(1500L));
    }

    @Test
    public void mkdir() throws Exception {
       defaultVfs.mkdir(VirtualPath.parse("/a/c"));

       assertThat(sftpRootDir.resolve("a/c")).isDirectory();
    }

    @Test
    public void rmdir() throws Exception {
        assertThat(sftpRootDir.resolve("a")).exists();

        defaultVfs.rmdir(VirtualPath.parse("/a"));

        assertThat(sftpRootDir.resolve("a")).doesNotExist();
    }

    @Test
    public void rm() throws Exception {
        assertThat(sftpRootDir.resolve("root.txt")).exists();

        defaultVfs.rm(VirtualPath.parse("/root.txt"));

        assertThat(sftpRootDir.resolve("root.txt")).doesNotExist();
    }

    @Test
    public void stat() throws Exception {
        final VirtualPath vpWithStat = defaultVfs.stat(VirtualPath.parse("/root.txt"));

        assertThat(vpWithStat.getStat().getType()).isEqualTo(VirtualFileType.FILE);
        assertThat(vpWithStat.getStat().getSize()).isEqualTo(Files.size(sftpRootDir.resolve("root.txt")));
        assertThat(vpWithStat.getStat().getModifiedTime()).isCloseTo(Files.getLastModifiedTime(sftpRootDir.resolve("root.txt")).toMillis(), within(1500L));

        // permissions
        assertThat(vpWithStat.getStat().getPermissions()).isGreaterThan(0);
        // if we got rid of the extra stuff stacked by sftp, this value should be less than this
        assertThat(vpWithStat.getStat().getPermissions()).isLessThan(4096);
    }

    @Test
    public void exists() throws Exception {
        final VirtualPath exists = defaultVfs.exists(VirtualPath.parse("/root.txt"));
        final VirtualPath notExists = defaultVfs.exists(VirtualPath.parse("/not-exists.txt"));

        assertThat(exists).isNotNull();
        assertThat(notExists).isNull();
    }

}