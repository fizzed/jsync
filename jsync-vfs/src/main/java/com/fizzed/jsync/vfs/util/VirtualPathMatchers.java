package com.fizzed.jsync.vfs.util;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;

public class VirtualPathMatchers {

    

    static public PathMatcher compileRule(String rule) {
        String glob = rule.trim();

        // 1. Handle directory-only rules (e.g., "build/")
        // Git implies "everything inside this directory"
        if (glob.endsWith("/")) {
            glob = glob + "**";
        }

        // 2. Handle "rooted" vs "anywhere" rules
        // If it starts with '/', it matches from the root only.
        // If NOT, it matches anywhere (e.g., "*.log" -> "** /*.log")
        if (glob.startsWith("/")) {
            // Remove leading slash for Java PathMatcher consistency on relative paths
            glob = glob.substring(1);
        } else {
            // If it's not rooted, allow it to match deep in the tree
            // Example: "tmp" becomes "**/tmp"
            if (!glob.startsWith("**/") && !glob.equals("*")) {
                glob = "**/" + glob;
            }
        }

        // Create the matcher using "glob" syntax
        return FileSystems.getDefault().getPathMatcher("glob:" + glob);
    }

}