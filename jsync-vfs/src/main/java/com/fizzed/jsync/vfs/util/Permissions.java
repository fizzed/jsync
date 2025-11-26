package com.fizzed.jsync.vfs.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public class Permissions {

    /**
     * Converts a set of {@link PosixFilePermission} to its corresponding POSIX integer representation.
     * The resulting integer is a bitmask representing the file permission mode.
     *
     * @param permissions the set of {@link PosixFilePermission} to convert; if null, the result will be 0
     * @return an integer representing the POSIX file permission mode derived from the given set of permissions
     */
    static public int toPosixInt(Set<PosixFilePermission> permissions) {
        int mode = 0;

        // Null check safety
        if (permissions == null) return mode;

        for (PosixFilePermission perm : permissions) {
            switch (perm) {
                case OWNER_READ:     mode |= 0400; break; // 256 in decimal
                case OWNER_WRITE:    mode |= 0200; break; // 128
                case OWNER_EXECUTE:  mode |= 0100; break; // 64

                case GROUP_READ:     mode |= 0040; break; // 32
                case GROUP_WRITE:    mode |= 0020; break; // 16
                case GROUP_EXECUTE:  mode |= 0010; break; // 8

                case OTHERS_READ:    mode |= 0004; break; // 4
                case OTHERS_WRITE:   mode |= 0002; break; // 2
                case OTHERS_EXECUTE: mode |= 0001; break; // 1
            }
        }

        return mode;
    }

    /**
     * Converts a POSIX file permission integer into a set of {@link PosixFilePermission}.
     * The input integer is interpreted as the POSIX file permission bitmask, where each bit
     * corresponds to a specific owner, group, or others permission (read, write, execute).
     *
     * @param permissions the integer representing the POSIX file permission bitmask
     * @return a set of {@link PosixFilePermission} that represents the provided integer bitmask
     */
    public static Set<PosixFilePermission> toPosixFilePermissions(int permissions) {
        // Create an empty set specifically for this Enum type
        Set<PosixFilePermission> perms = EnumSet.noneOf(PosixFilePermission.class);

        // Owner (User)
        if ((permissions & 0400) != 0) perms.add(PosixFilePermission.OWNER_READ);
        if ((permissions & 0200) != 0) perms.add(PosixFilePermission.OWNER_WRITE);
        if ((permissions & 0100) != 0) perms.add(PosixFilePermission.OWNER_EXECUTE);

        // Group
        if ((permissions & 0040) != 0) perms.add(PosixFilePermission.GROUP_READ);
        if ((permissions & 0020) != 0) perms.add(PosixFilePermission.GROUP_WRITE);
        if ((permissions & 0010) != 0) perms.add(PosixFilePermission.GROUP_EXECUTE);

        // Others (World)
        if ((permissions & 0004) != 0) perms.add(PosixFilePermission.OTHERS_READ);
        if ((permissions & 0002) != 0) perms.add(PosixFilePermission.OTHERS_WRITE);
        if ((permissions & 0001) != 0) perms.add(PosixFilePermission.OTHERS_EXECUTE);

        return perms;
    }

    static public Set<PosixFilePermission> getPosixPermissions(Path path) throws IOException {
        // 2. On Windows: Synthesize permissions based on "DOS" attributes
        Set<PosixFilePermission> perms = new HashSet<>();

        // We use the basic Files.check methods which abstract the OS details
        boolean isReadable = Files.isReadable(path);
        boolean isWritable = Files.isWritable(path);
        boolean isExecutable = Files.isExecutable(path);

        // it seems like on windows, its sftp server only really sets the "owner" permissions and uses zeroes for the rest
        if (isReadable)   perms.add(PosixFilePermission.OWNER_READ);
        if (isWritable)   perms.add(PosixFilePermission.OWNER_WRITE);
        if (isExecutable) perms.add(PosixFilePermission.OWNER_EXECUTE);

        // --- GROUP (Mirror Owner or Default to Read) ---
        //if (isReadable)   perms.add(PosixFilePermission.GROUP_READ);
        //if (isWritable)   perms.add(PosixFilePermission.GROUP_WRITE); // Optional: usually safer to omit on Windows
        //if (isExecutable) perms.add(PosixFilePermission.GROUP_EXECUTE);

        // --- OTHERS (Mirror Owner or Default to Read) ---
        //if (isReadable)   perms.add(PosixFilePermission.OTHERS_READ);
        //if (isExecutable) perms.add(PosixFilePermission.OTHERS_EXECUTE);
        // Usually we don't give OTHERS_WRITE on a best-effort basis

        return perms;
    }

}