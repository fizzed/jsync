package com.fizzed.jsync.vfs;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

abstract public class AbstractVirtualFileSystem implements VirtualFileSystem {

    protected final String name;
    protected final VirtualPath pwd;
    protected final boolean caseSensitive;
    protected volatile Set<Checksum> checksums;

    public AbstractVirtualFileSystem(String name, VirtualPath pwd, boolean caseSensitive) {
        this.name = name;
        this.pwd = pwd;
        this.caseSensitive = caseSensitive;
        this.checksums = null;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public VirtualPath pwd() {
        return this.pwd;
    }

    @Override
    public boolean isCaseSensitive() {
        return this.caseSensitive;
    }

    @Override
    public boolean isChecksumSupported(Checksum checksum) throws IOException {
        return this.getChecksumsSupported().contains(checksum);
    }

    @Override
    public Set<Checksum> getChecksumsSupported() throws IOException {
        if (this.checksums == null) {
            // we need to calculate them
            synchronized (this) {
                if (this.checksums == null) {
                    final List<Checksum> values = this.doDetectChecksums();
                    this.checksums = new LinkedHashSet<>(values);
                }
            }
        }
        return this.checksums;
    }

    abstract protected List<Checksum> doDetectChecksums() throws IOException;

    @Override
    public String toString() {
        return this.name;
    }

}