package com.fizzed.jsync.engine;

import com.fizzed.jsync.vfs.StatUpdateOption;
import com.fizzed.jsync.vfs.VirtualFileSystem;
import com.fizzed.jsync.vfs.VirtualPath;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Set;

public interface JsyncEventHandler {

    void willBegin(VirtualFileSystem sourceVfs, VirtualPath sourcePath, VirtualFileSystem targetVfs, VirtualPath targetPath);

    void willEnd(VirtualFileSystem sourceVfs, VirtualPath sourcePath, VirtualFileSystem targetVfs, VirtualPath targetPath, JsyncResult result, long timeMillis);

    void willExcludePath(VirtualPath sourcePath);

    void willIgnoreSourcePath(VirtualPath sourcePath);

    void willIgnoreTargetPath(VirtualPath targetPath);

    void willCreateDirectory(VirtualPath targetPath, boolean recursively);

    void willDeleteDirectory(VirtualPath targetPath, boolean recursively);

    void willDeleteFile(VirtualPath targetPath, boolean recursively);

    void willTransferFile(VirtualPath sourcePath, VirtualPath targetPath, JsyncPathChanges changes);

    void willUpdateStat(VirtualPath sourcePath, VirtualPath targetPath, JsyncPathChanges changes, Collection<StatUpdateOption> options, boolean associatedWithFileModifiedOrDirCreated);

    void doCopy(InputStream input, OutputStream output, long knownContentLength) throws IOException;

}