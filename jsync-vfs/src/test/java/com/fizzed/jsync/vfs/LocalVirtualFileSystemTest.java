package com.fizzed.jsync.vfs;

import com.fizzed.crux.util.MoreFiles;
import com.fizzed.crux.util.Resources;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LocalVirtualFileSystemTest {

    static Path projectDir;
    private Path sourceDir;
    private LocalVirtualFileSystem defaultVfs;

    @BeforeAll
    static public void setup() throws Exception {
        projectDir = Resources.file("/locator.txt").resolve("../..").toAbsolutePath().normalize();
    }

    @BeforeEach
    public void before() throws IOException {
        this.sourceDir = projectDir.resolve("local-vfs-source");
        MoreFiles.deleteDirectoryIfExists(this.sourceDir);
        Files.createDirectories(this.sourceDir);
        this.defaultVfs = LocalVirtualFileSystem.open(this.sourceDir);
    }

    @Test
    public void permissions() throws Exception {
        // we want to use a .txt file, which isn't naturally executable on windows or linux/mac
        Path file = this.sourceDir.resolve("test.txt");
        Files.write(file, "#!/bin/sh\necho hello".getBytes());

        if (this.defaultVfs.getStatModel() == StatModel.POSIX) {
            // set permissions to 755 (rwxr-xr-x)
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwxr-xr-x"));

            final VirtualPath fileWithStat = this.defaultVfs.stat(VirtualPath.parse(file.toString()));

            assertThat(fileWithStat.getStat().getPermissionsOctal()).isEqualTo("755");
        } else {
            // on windows this will always end up returning 700
            final VirtualPath fileWithStat = this.defaultVfs.stat(VirtualPath.parse(file.toString()));

            assertThat(fileWithStat.getStat().getPermissionsOctal()).isEqualTo("700");
        }
    }

}