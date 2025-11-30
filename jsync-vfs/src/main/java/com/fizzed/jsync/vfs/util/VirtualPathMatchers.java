package com.fizzed.jsync.vfs.util;

import com.fizzed.jsync.vfs.VirtualPath;

import java.util.ArrayList;
import java.util.List;

public class VirtualPathMatchers {

    private final List<VirtualPathMatcher> matchers = new ArrayList<>();

    public VirtualPathMatchers(List<VirtualPathMatcher> matchers) {
        this.matchers.addAll(matchers);
    }

    public boolean matches(VirtualPath rootPath, VirtualPath path) {
        for  (VirtualPathMatcher matcher : matchers) {
            if (matcher.matches(rootPath, path)) {
                return true;
            }
        }
        return false;
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