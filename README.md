# Jsync by Fizzed

[![Maven Central](https://img.shields.io/maven-central/v/com.fizzed/jsync?style=flat-square)](https://mvnrepository.com/artifact/com.fizzed/jsync)

## Automated Testing

The following Java versions and platforms are tested using GitHub workflows:

[![Java 8](https://img.shields.io/github/actions/workflow/status/fizzed/jsync/linux-java8.yaml?branch=master&label=Java%208&style=flat-square)](https://github.com/fizzed/jsync/actions/workflows/linux-java8.yaml)
[![Java 11](https://img.shields.io/github/actions/workflow/status/fizzed/jsync/linux-java11.yaml?branch=master&label=Java%2011&style=flat-square)](https://github.com/fizzed/jsync/actions/workflows/linux-java11.yaml)
[![Java 17](https://img.shields.io/github/actions/workflow/status/fizzed/jsync/linux-java17.yaml?branch=master&label=Java%2017&style=flat-square)](https://github.com/fizzed/jsync/actions/workflows/linux-java17.yaml)
[![Java 21](https://img.shields.io/github/actions/workflow/status/fizzed/jsync/linux-java21.yaml?branch=master&label=Java%2021&style=flat-square)](https://github.com/fizzed/jsync/actions/workflows/linux-java21.yaml)
[![Java 25](https://img.shields.io/github/actions/workflow/status/fizzed/jsync/linux-java25.yaml?branch=master&label=Java%2025&style=flat-square)](https://github.com/fizzed/jsync/actions/workflows/linux-java25.yaml)

[![Linux x64](https://img.shields.io/github/actions/workflow/status/fizzed/jsync/linux-java8.yaml?branch=master&label=Linux%20x64&style=flat-square)](https://github.com/fizzed/jsync/actions/workflows/linux-java8.yaml)
[![MacOS arm64](https://img.shields.io/github/actions/workflow/status/fizzed/jsync/macos-arm64.yaml?branch=master&label=MacOS%20arm64&style=flat-square)](https://github.com/fizzed/jsync/actions/workflows/macos-arm64.yaml)
[![Windows x64](https://img.shields.io/github/actions/workflow/status/fizzed/jsync/windows-x64.yaml?branch=master&label=Windows%20x64&style=flat-square)](https://github.com/fizzed/jsync/actions/workflows/windows-x64.yaml)

The following platforms are tested using the [Fizzed, Inc.](http://fizzed.com) build system:

[![Alpine x64](https://img.shields.io/badge/Alpine_x64-passing-brightgreen?style=flat-square)](buildx-results.txt)
[![Alpine arm64](https://img.shields.io/badge/Alpine_arm64-passing-brightgreen?style=flat-square)](buildx-results.txt)
[![Alpine riscv64](https://img.shields.io/badge/Alpine_riscv64-passing-brightgreen?style=flat-square)](buildx-results.txt)
[![FreeBSD x64](https://img.shields.io/badge/FreeBSD_x64-passing-brightgreen?style=flat-square)](buildx-results.txt)
[![FreeBSD arm64](https://img.shields.io/badge/FreeBSD_arm64-passing-brightgreen?style=flat-square)](buildx-results.txt)
[![Linux arm64](https://img.shields.io/badge/Linux_arm64-passing-brightgreen?style=flat-square)](buildx-results.txt)
[![Linux riscv64](https://img.shields.io/badge/Linux_riscv64-passing-brightgreen?style=flat-square)](buildx-results.txt)
[![MacOS x64](https://img.shields.io/badge/MacOS_x64-passing-brightgreen?style=flat-square)](buildx-results.txt)
[![NetBSD x64](https://img.shields.io/badge/MacOS_x64-passing-brightgreen?style=flat-square)](buildx-results.txt)
[![OpenBSD x64](https://img.shields.io/badge/OpenBSD_x64-passing-brightgreen?style=flat-square)](buildx-results.txt)
[![OpenBSD arm64](https://img.shields.io/badge/OpenBSD_arm64-passing-brightgreen?style=flat-square)](buildx-results.txt)
[![Windows arm64](https://img.shields.io/badge/Windows_arm64-passing-brightgreen?style=flat-square)](buildx-results.txt)

## Overview

Pure Java library (8, 11, 17, 21, 25, etc.) for providing rsync-like efficient file synchronization between two directories
or files either locally or remotely via SSH/SFTP. Requires no native dependencies, works on all major platforms including Windows,
and requires no special executables present on the remote system.

## Features
 - Supports all platforms that Java can run on, including Windows
 - Diff-based Syncing: Only transfers files that have changed (based on size, modification time, or checksum).
 - Local: Sync between local directories.
 - SSH/SFTP: Sync between local and remote servers (via SSH/SFTP).
 - Zero-Dependency Core: The core logic is separated from protocol implementations to keep the footprint small.
 - Builder API: Fluent, easy-to-use Java API for configuring sync jobs.
 - Supports syncing file and directory permissions
 - Does NOT require rsync to be installed on either system, uses SFTP for file operations on the remote system,
along with using SSH for checksums if needed.
 - Leverages the excellent [Jsch](https://github.com/mwiede/jsch) library for SSH/SFTP support (but designed to pluggable
and support other SSH/SFTP implementations in the future)

## Command-Line Tool / Example

This library is optimized primarily for programmatic use, but if you'd like to simply use it from the command-line,
or to quickly give it a try, we suggest trying it out from within the [Blaze Script System](https://github.com/fizzed/blaze)
which has a built-in Jsync plugin with sophisticated SSH session handling (such as supporting ~/.ssh/config, ssh agents, etc.)
To quickly give it a try, you can run the following commands:

```shell
git clone https://github.com/fizzed/blaze.git
cd blaze
java -jar blaze.jar examples/jsync.java --from ~/example.iso --to bmh-build-x64-freebsd15-1:. --mode nest
```

Not all options are available from the command-line, but many of the most common ones are supported. If there is enough
demand, we may add a standalone command-line tool in the future.

## Usage

Published to maven central use the following for core and local-only syncing:

```xml
<dependency>
    <groupId>com.fizzed</groupId>
    <artifactId>jsync-engine</artifactId>
    <version>1.5.0</version>
</dependency>
```

If you'd like to use SSH/SFTP syncing, you'll need to add the following dependency as well:

```xml
<dependency>
    <groupId>com.fizzed</groupId>
    <artifactId>jsync-sftp</artifactId>
    <version>1.5.0</version>
</dependency>
```

To sync between two directories locally, here is an example:

```java
import com.fizzed.jsync.vfs.VirtualVolume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Paths;
import static com.fizzed.jsync.vfs.LocalVirtualVolume.localVolume;

public class LocalToLocalDemo {
    static private final Logger log = LoggerFactory.getLogger(LocalToLocalDemo.class);

    static public void main(String[] args) throws Exception {

        final VirtualVolume source = localVolume(Paths.get("example/source"));
        final VirtualVolume target = localVolume(Paths.get("example/target"));

        final JsyncResult result = new JsyncEngine()
            .setDelete(true)
            .sync(source, target, JsyncMode.MERGE);
    }

}
```

To sync between a local and remote directory, here is an example. Note that this includes more options to control
how the sync is performed, such as telling the engine to create parent directories if they don't exist, and to force
the sync if the target directory has "file type" mismatches (i.e. a file exists on the target with the same name as a
directory on the source, or vice versa), and to ignore syncing the local '.git' directory.

```java
import com.fizzed.jsync.vfs.VirtualVolume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Paths;
import static com.fizzed.jsync.sftp.SftpVirtualVolume.sftpVolume;
import static com.fizzed.jsync.vfs.LocalVirtualVolume.localVolume;

public class LocalToRemoteDemo {
    static private final Logger log = LoggerFactory.getLogger(LocalToRemoteDemo.class);

    static public void main(String[] args) throws Exception {

        final VirtualVolume source = localVolume(Paths.get("example/source"));
        final VirtualVolume target = sftpVolume("target-host", "remote-dir/target");

        final JsyncResult result = new JsyncEngine()
            .setDelete(true)
            .setParents(true)
            .setForce(true)
            .addIgnore(".git")
            .sync(source, target, JsyncMode.MERGE);
    }

}
```

The sftp module provides fairly basic support for SSH configuration with just a hostname. Ideally, you'd provide your
own Jsch SSH session to your `sftpVolume` call, where you can then provide your own custom SSH configuration. For example:

```java
import com.fizzed.jsync.vfs.VirtualVolume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Paths;
import static com.fizzed.jsync.sftp.SftpVirtualVolume.sftpVolume;
import static com.fizzed.jsync.vfs.LocalVirtualVolume.localVolume;

public class LocalToRemoteDemo {
    static private final Logger log = LoggerFactory.getLogger(LocalToRemoteDemo.class);

    static public void main(String[] args) throws Exception {

        // create a jsch session with custom configuration
        final com.jcraft.jsch.Session ssh = createMyOwnSshSession();
        
        final VirtualVolume source = localVolume(Paths.get("example/source"));
        final VirtualVolume target = sftpVolume(ssh, "remote-dir/target");

        final JsyncResult result = new JsyncEngine()
            .sync(source, target, JsyncMode.MERGE);
    }

}
```

If you want more sophisticated SSH sessions automatically handled by this Jsync engine, you should look at using
the [Blaze Script System](https://github.com/fizzed/blaze), which has a built-in Jsync plugin that supports SSH sessions with sophisticated configuration
(such as ~/.ssh/config, ssh agents, etc.).

## Sponsorship & Support

![](https://cdn.fizzed.com/github/fizzed-logo-100.png)

Project by [Fizzed, Inc.](http://fizzed.com) (Follow on Twitter: [@fizzed_inc](http://twitter.com/fizzed_inc))

**Developing and maintaining opensource projects requires significant time.** If you find this project useful or need
commercial support, we'd love to chat. Drop us an email at [ping@fizzed.com](mailto:ping@fizzed.com)

Project sponsors may include the following benefits:

- Priority support (outside of Github)
- Feature development & roadmap
- Priority bug fixes
- Privately hosted continuous integration tests for their unique edge or use cases

## License

Copyright (C) 2025+ Fizzed, Inc.

This work is licensed under the Apache License, Version 2.0. See LICENSE for details.
