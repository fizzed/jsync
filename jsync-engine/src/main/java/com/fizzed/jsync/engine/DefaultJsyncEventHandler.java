package com.fizzed.jsync.engine;

import com.fizzed.jsync.vfs.StatUpdateOption;
import com.fizzed.jsync.vfs.VirtualFileSystem;
import com.fizzed.jsync.vfs.VirtualPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import static java.util.stream.Collectors.joining;

public class DefaultJsyncEventHandler implements JsyncEventHandler {
    // we use a logger as though it was for jsync engine and not us
    static private final Logger log = LoggerFactory.getLogger(JsyncEngine.class);

    @Override
    public void willBegin(VirtualFileSystem sourceVfs, VirtualPath sourcePath, VirtualFileSystem targetVfs, VirtualPath targetPath) {
        log.debug("Syncing {}:{} -> {}:{}", sourceVfs, sourcePath, targetVfs, targetPath);
    }

    @Override
    public void willEnd(VirtualFileSystem sourceVfs, VirtualPath sourcePath, VirtualFileSystem targetVfs, VirtualPath targetPath, JsyncResult result, long timeMillis) {
        log.debug("Synced {} new {} updated {} deleted files, {} new {} deleted dirs (in {} ms)", result.getFilesCreated(),
            result.getFilesUpdated(), result.getFilesDeleted(),result.getDirsCreated(), result.getDirsDeleted(), timeMillis);
    }

    @Override
    public void willExcludePath(VirtualPath sourcePath) {
        log.debug("Excluding path {}", sourcePath);
    }

    @Override
    public void willIgnorePath(VirtualPath sourcePath) {
        log.debug("Ignoring path {}", sourcePath);
    }

    @Override
    public void willCreateDirectory(VirtualPath targetPath, boolean recursively) {
        log.debug("Creating directory {}", targetPath);
    }

    @Override
    public void willDeleteDirectory(VirtualPath targetPath, boolean recursively) {
        log.debug("Deleting directory {}", targetPath);
    }

    @Override
    public void willDeleteFile(VirtualPath targetPath, boolean recursively) {
        log.debug("Deleting file {}", targetPath);
    }

    @Override
    public void willTransferFile(VirtualPath sourcePath, VirtualPath targetPath, JsyncPathChanges changes) {
        if (changes.isMissing()) {
            log.debug("Creating file {} ({})", targetPath, changes);
        } else {
            log.debug("Updating file {}", targetPath);
        }
    }

    @Override
    public void willUpdateStat(VirtualPath sourcePath, VirtualPath targetPath, JsyncPathChanges changes, Collection<StatUpdateOption> options, boolean associatedWithFileModifiedOrDirCreated) {
        String message = options.stream().map(v -> v.name().toLowerCase()).collect(joining(","));
        log.debug("Updating stat {} ({})", targetPath, message);
    }

    static private final int BUFFER_SIZE = 8192;

    @Override
    public void doCopy(InputStream input, OutputStream output, long knownContentLength) throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        int n;
        while ((n = input.read(buf)) >= 0) {
            output.write(buf, 0, n);
        }
    }

}