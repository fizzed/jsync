package com.fizzed.jsync.vfs.util;

import com.fizzed.jsync.vfs.VirtualPath;

import java.util.ArrayList;
import java.util.List;

public class VirtualPathMatchers {

    private final List<VirtualPathMatcher> matchers;

    public VirtualPathMatchers(List<VirtualPathMatcher> matchers) {
        this.matchers = matchers;
    }

    public boolean matches(VirtualPath rootPath, VirtualPath path) {
        for  (VirtualPathMatcher matcher : this.matchers) {
            if (matcher.matches(rootPath, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return this.matchers.toString();
    }

    static public VirtualPathMatchers compile(List<String> rules) {
        List<VirtualPathMatcher> matchers = new ArrayList<>();
        if (rules != null) {
            for (String rule : rules) {
                matchers.add(VirtualPathMatcher.compile(rule));
            }
        }
        return new VirtualPathMatchers(matchers);
    }

}