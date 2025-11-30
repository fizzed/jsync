package com.fizzed.jsync.vfs.util;

import com.fizzed.jsync.vfs.VirtualPath;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualPathMatcherTest {

    @Test
    public void absoluteRootOnlyRuleForDirectory() {
        VirtualPath rootPath = VirtualPath.parse("/home/jjlauer");

        VirtualPathMatcher matcher = VirtualPathMatcher.compile("/target");

        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/target"))).isTrue();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/target/a.txt"))).isFalse();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/target2"))).isFalse();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/2target"))).isFalse();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/sub/target"))).isFalse();
    }

    @Test
    public void absoluteAnywhereRuleForDirectory() {
        VirtualPath rootPath = VirtualPath.parse("/home/jjlauer");

        VirtualPathMatcher matcher = VirtualPathMatcher.compile("target");

        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/target"))).isTrue();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/target/a.txt"))).isFalse();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/target2"))).isFalse();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/2target"))).isFalse();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/sub/target"))).isTrue();
    }

    @Test
    public void absoluteAnywhereRuleForDirectoryWithSlash() {
        VirtualPath rootPath = VirtualPath.parse("/home/jjlauer");

        VirtualPathMatcher matcher = VirtualPathMatcher.compile("target/");

        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/target"))).isTrue();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/target/a.txt"))).isTrue();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/target2"))).isFalse();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/2target"))).isFalse();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/sub/target"))).isTrue();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/sub/target/a.txt"))).isTrue();
    }

    @Test
    public void absoluteAnywhereRuleForFile() {
        VirtualPath rootPath = VirtualPath.parse("/home/jjlauer");

        VirtualPathMatcher matcher = VirtualPathMatcher.compile("*.log");

        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/target"))).isFalse();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/log"))).isFalse();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/a.log"))).isTrue();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("/home/jjlauer/sub/a.log"))).isTrue();
    }

    @Test
    public void absoluteAnywhereRuleForDirectoryOnWindows() {
        VirtualPath rootPath = VirtualPath.parse("C:\\Users\\jjlauer");

        VirtualPathMatcher matcher = VirtualPathMatcher.compile("target");

        assertThat(matcher.matches(rootPath, VirtualPath.parse("C:\\Users\\jjlauer\\target"))).isTrue();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("C:\\Users\\jjlauer\\target\\a.txt"))).isFalse();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("C:\\Users\\jjlauer\\target2"))).isFalse();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("C:\\Users\\jjlauer\\2target"))).isFalse();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("C:\\Users\\jjlauer\\sub\\target"))).isTrue();
    }

    @Test
    public void relativeDoubleStarRuleForFiles() {
        VirtualPath rootPath = VirtualPath.parse("/home/jjlauer");

        VirtualPathMatcher matcher = VirtualPathMatcher.compile("docs/**/*.md");

        // this is where java's globbing kinda fails where it can't do zero or more with that match
        //assertThat(matcher.matches(rootPath, VirtualPath.parse("docs"))).isFalse();
        assertThat(matcher.matches(rootPath, VirtualPath.parse("docs/src/a.md"))).isTrue();
    }

}