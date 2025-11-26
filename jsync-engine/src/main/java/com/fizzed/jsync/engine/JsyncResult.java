package com.fizzed.jsync.engine;

public class JsyncResult {

    final private JsyncMode mode;
    private int checksums;
    private int filesCreated;
    private int filesUpdated;
    private int filesDeleted;
    private int dirsCreated;
    private int dirsDeleted;
    private int statsUpdated;

    public JsyncResult(JsyncMode mode) {
        this.mode = mode;
    }

    public JsyncMode getMode() {
        return mode;
    }

    public int getChecksums() {
        return checksums;
    }

    public int getFilesCreated() {
        return filesCreated;
    }

    public int getFilesUpdated() {
        return filesUpdated;
    }

    public int getFilesDeleted() {
        return filesDeleted;
    }

    public int getDirsCreated() {
        return dirsCreated;
    }

    public int getDirsDeleted() {
        return dirsDeleted;
    }

    public int getStatsUpdated() {
        return statsUpdated;
    }

    // helpers

    public int getFilesTotal() {
        return this.filesCreated + this.filesDeleted + this.filesUpdated;
    }

    public int getDirsTotal() {
        return this.dirsCreated + this.dirsDeleted;
    }

    public int getStatsOnlyUpdated() {
        int totalNewOrUpdated = this.filesCreated + this.filesUpdated + this.dirsDeleted;
        int statsOnlyUpdated = this.statsUpdated - totalNewOrUpdated;
        // this shouldn't happen, but if it does, just make it zero
        if (statsOnlyUpdated < 0) {
            statsOnlyUpdated = 0;
        }
        return statsOnlyUpdated;
    }

    // increment methods for all

    public void incrementChecksums(int amount) {
        checksums += amount;
    }

    public void incrementFilesCreated() {
        filesCreated++;
    }

    public void incrementFilesUpdated() {
        filesUpdated++;
    }

    public void incrementFilesDeleted() {
        filesDeleted++;
    }

    public void incrementDirsCreated() {
        dirsCreated++;
    }

    public void incrementDirsDeleted() {
        dirsDeleted++;
    }

    public void incrementStatsUpdated() {
        statsUpdated++;
    }

    @Override
    public String toString() {
        return "checksums=" + checksums + ", filesCreated=" + filesCreated + ", filesUpdated=" + filesUpdated + ", filesDeleted=" + filesDeleted + ", dirsCreated=" + dirsCreated + ", dirsDeleted=" + dirsDeleted + ", statsUpdated=" + statsUpdated;
    }

}