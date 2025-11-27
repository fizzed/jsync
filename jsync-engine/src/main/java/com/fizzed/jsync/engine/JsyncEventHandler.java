package com.fizzed.jsync.engine;

import com.fizzed.jsync.vfs.VirtualFileSystem;
import com.fizzed.jsync.vfs.VirtualPath;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface JsyncEventHandler {

    void willBegin(VirtualFileSystem sourceVfs, VirtualPath sourcePath, VirtualFileSystem targetVfs, VirtualPath targetPath);

    void willEnd(VirtualFileSystem sourceVfs, VirtualPath sourcePath, VirtualFileSystem targetVfs, VirtualPath targetPath, JsyncResult result, long timeMillis);

    void willExcludePath(VirtualPath targetPath);

    void willIgnorePath(VirtualPath targetPath);

    void willCreateDirectory(VirtualPath targetPath, boolean recursively);

    void willDeleteDirectory(VirtualPath targetPath, boolean recursively);

    void willDeleteFile(VirtualPath targetPath, boolean recursively);

    void willTransferFile(VirtualPath sourcePath, VirtualPath targetPath, JsyncPathChanges changes);

    void willUpdateStat(VirtualPath sourcePath, VirtualPath targetPath, JsyncPathChanges changes, boolean associatedWithFileModifiedOrDirCreated);

    void doCopy(InputStream input, OutputStream output, long knownContentLength) throws IOException;

}