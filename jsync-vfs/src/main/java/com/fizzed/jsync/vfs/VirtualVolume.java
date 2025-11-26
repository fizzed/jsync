package com.fizzed.jsync.vfs;

import java.io.IOException;

public interface VirtualVolume {

    String getPath();

    VirtualFileSystem openFileSystem() throws IOException;

}