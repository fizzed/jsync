package com.fizzed.jsync.vfs.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IoHelper {

    static private final int BUFFER_SIZE = 8192; // 8KB

    static public long copy(InputStream input, OutputStream output) throws IOException {
        long nread = 0L;
        byte[] buf = new byte[BUFFER_SIZE];
        int n;
        while ((n = input.read(buf)) > 0) {
            output.write(buf, 0, n);
            nread += n;
        }
        return nread;
    }

}