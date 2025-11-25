package com.fizzed.jsync.vfs;

public interface VirtualVolume {

    String getPath();

    VirtualFileSystem openFileSystem();

}