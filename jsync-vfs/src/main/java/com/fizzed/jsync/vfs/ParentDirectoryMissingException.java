package com.fizzed.jsync.vfs;

import java.io.IOException;

public class ParentDirectoryMissingException extends IOException {
    public ParentDirectoryMissingException(String message) {
        super(message);
    }
}