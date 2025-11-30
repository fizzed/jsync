package com.fizzed.jsync.vfs.util;

import com.fizzed.jsync.vfs.VirtualPath;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

public class VirtualPathMatcher {

    private final PathMatcher matcher;

    public VirtualPathMatcher(PathMatcher matcher) {
        this.matcher = matcher;
    }

    public boolean matches(VirtualPath rootPath, VirtualPath currentPath) {
        final String relativePath;

        // resolve the current path against the root path, so we're left with the relative path we're matching against
        if (currentPath.isAbsolute()) {
            String rootFullPath = rootPath.toFullPath();
            String currentFullPath = currentPath.toFullPath();
            int pathStartPos = currentFullPath.indexOf(rootFullPath);
            if (pathStartPos >= 0) {
                // remove the leading path PLUS the file separator
                relativePath = currentFullPath.substring(pathStartPos + rootFullPath.length() + 1);
            } else {
                relativePath = currentFullPath;
            }
        } else {
            relativePath = currentPath.toString();
        }

        return this.matcher.matches(Paths.get(relativePath));
    }

    static public VirtualPathMatcher compile(String rule) {
        String glob = rule.trim();
        boolean isDirectory = false;
        boolean isRooted = false;

        // 1. Check for Directory marker
        if (glob.endsWith("/")) {
            isDirectory = true;
            glob = glob.substring(0, glob.length() - 1); // Strip trailing slash
        }

        // 2. Check for Root anchor
        if (glob.startsWith("/")) {
            isRooted = true;
            glob = glob.substring(1); // Strip leading slash
        }

        // FIX: Handle "/**/" usually found in the middle of paths
        // Git: "docs/**/*.md" -> Zero or more dirs
        // Java: "docs/**/*.md" -> One or more dirs (fails on docs/file.md)
        // Solution: Replace "/**/" with "/{,**/}" which means "Empty OR /**/"
//        if (glob.contains("/**/")) {
//            glob = glob.replace("/**/", "/{,**/}");
//        }

        // 3. Build the Glob
        // We need to construct a robust brace expansion {A,B,C...}
        StringBuilder finalGlob = new StringBuilder();
        finalGlob.append("glob:{");

        if (isRooted) {
            // Rule: /target/ or /target
            finalGlob.append(glob); // Matches "target" at root
            if (isDirectory) {
                finalGlob.append(",").append(glob).append("/**"); // Matches "target/..." at root
            }
        } else {
            // Rule: target/ or target
            finalGlob.append(glob);              // Matches "target" at root
            finalGlob.append(",**/").append(glob); // Matches "src/target" (nested)

            if (isDirectory) {
                // If it's a directory, we must ALSO match the contents
                finalGlob.append(",").append(glob).append("/**");       // Matches "target/file.txt" (root)
                finalGlob.append(",**/").append(glob).append("/**");    // Matches "src/target/file.txt" (nested)
            }
        }

        finalGlob.append("}");

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(finalGlob.toString());

        return new  VirtualPathMatcher(matcher);
    }

}