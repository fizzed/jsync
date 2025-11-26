package com.fizzed.jsync.sftp.impl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

public class SignalClosingOutputStream extends FilterOutputStream {
    private final CountDownLatch latch;

    public SignalClosingOutputStream(OutputStream out, CountDownLatch latch) {
        super(out);
        this.latch = latch;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
    }

    @Override
    public void close() throws IOException {
        try {
            // This is the "Signal" logic
            if (latch != null) {
                latch.countDown();
            }
        } finally {
            // Ensure the underlying stream is actually closed (if desired)
            super.close();
        }
    }

}