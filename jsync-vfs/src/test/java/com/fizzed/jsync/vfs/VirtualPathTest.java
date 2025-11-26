package com.fizzed.jsync.vfs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualPathTest {

    @Test
    public void relative() {
        VirtualPath vp = new VirtualPath(null, "a", false, null);

        assertThat(vp.getParentPath()).isNull();
        assertThat(vp.getName()).isEqualTo("a");
        assertThat(vp.isDirectory()).isFalse();
        assertThat(vp.toFullPath()).isEqualTo("a");
        assertThat(vp.isAbsolute()).isFalse();
        assertThat(vp.isRelative()).isTrue();

        vp = VirtualPath.parse("a", true);

        assertThat(vp.getParentPath()).isNull();
        assertThat(vp.getName()).isEqualTo("a");
        assertThat(vp.isDirectory()).isTrue();
        assertThat(vp.toFullPath()).isEqualTo("a");
        assertThat(vp.isAbsolute()).isFalse();
        assertThat(vp.isRelative()).isTrue();

        // resolve another relative with it
        VirtualPath vp2 = vp.resolve("b", true, null);

        assertThat(vp2.getParentPath()).isEqualTo("a");
        assertThat(vp2.getName()).isEqualTo("b");
        assertThat(vp2.isDirectory()).isTrue();
        assertThat(vp2.toFullPath()).isEqualTo("a/b");
        assertThat(vp2.isAbsolute()).isFalse();
        assertThat(vp2.isRelative()).isTrue();

        // resolve an absolute with it
        vp2 = vp.resolve("/b", true, null);

        assertThat(vp2.getParentPath()).isEqualTo("");
        assertThat(vp2.getName()).isEqualTo("b");
        assertThat(vp2.isDirectory()).isTrue();
        assertThat(vp2.toFullPath()).isEqualTo("/b");
        assertThat(vp2.isAbsolute()).isTrue();
        assertThat(vp2.isRelative()).isFalse();
    }

    @Test
    public void relativeWindows() {
        VirtualPath vp = VirtualPath.parse("a\\b", true);

        assertThat(vp.getParentPath()).isEqualTo("a");
        assertThat(vp.getName()).isEqualTo("b");
        assertThat(vp.isDirectory()).isTrue();
        assertThat(vp.toFullPath()).isEqualTo("a/b");
        assertThat(vp.isAbsolute()).isFalse();
        assertThat(vp.isRelative()).isTrue();
    }

    @Test
    public void absolute() {
        VirtualPath vp = new VirtualPath("", "a", false, null);

        assertThat(vp.getParentPath()).isEqualTo("");
        assertThat(vp.getName()).isEqualTo("a");
        assertThat(vp.isDirectory()).isFalse();
        assertThat(vp.toFullPath()).isEqualTo("/a");
        assertThat(vp.toString()).isEqualTo("/a");
        assertThat(vp.isAbsolute()).isTrue();
        assertThat(vp.isRelative()).isFalse();

        vp = VirtualPath.parse("/a", true);

        assertThat(vp.getParentPath()).isEqualTo("");
        assertThat(vp.getName()).isEqualTo("a");
        assertThat(vp.isDirectory()).isTrue();
        assertThat(vp.toFullPath()).isEqualTo("/a");
        assertThat(vp.toString()).isEqualTo("/a");
        assertThat(vp.isAbsolute()).isTrue();
        assertThat(vp.isRelative()).isFalse();

        // resolve another relative with it
        VirtualPath vp2 = vp.resolve("b", true, null);

        assertThat(vp2.getParentPath()).isEqualTo("/a");
        assertThat(vp2.getName()).isEqualTo("b");
        assertThat(vp2.isDirectory()).isTrue();
        assertThat(vp2.toFullPath()).isEqualTo("/a/b");
        assertThat(vp2.toFullPath()).isEqualTo("/a/b");
        assertThat(vp2.isAbsolute()).isTrue();
        assertThat(vp2.isRelative()).isFalse();

        // resolve another absolute with it
        vp2 = vp.resolve("/b", true, null);

        assertThat(vp2.getParentPath()).isEqualTo("");
        assertThat(vp2.getName()).isEqualTo("b");
        assertThat(vp2.isDirectory()).isTrue();
        assertThat(vp2.toFullPath()).isEqualTo("/b");
        assertThat(vp2.toFullPath()).isEqualTo("/b");
        assertThat(vp2.isAbsolute()).isTrue();
        assertThat(vp2.isRelative()).isFalse();
    }

    @Test
    public void rootPath() {
        VirtualPath vp = VirtualPath.parse("/", true);

        assertThat(vp.getParentPath()).isEqualTo("");
        assertThat(vp.getName()).isEqualTo("");
        assertThat(vp.isDirectory()).isTrue();
        assertThat(vp.toFullPath()).isEqualTo("/");
        assertThat(vp.toString()).isEqualTo("/");
        assertThat(vp.isAbsolute()).isTrue();
        assertThat(vp.isRelative()).isFalse();

        VirtualPath vp2 = vp.resolve("a", true, null);

        assertThat(vp2.getParentPath()).isEqualTo("");
        assertThat(vp2.getName()).isEqualTo("a");
        assertThat(vp2.toFullPath()).isEqualTo("/a");

        VirtualPath vp3 = vp2.resolve("b", true, null);

        assertThat(vp3.getParentPath()).isEqualTo("/a");
        assertThat(vp3.getName()).isEqualTo("b");
        assertThat(vp3.toFullPath()).isEqualTo("/a/b");
    }

    @Test
    public void rootPathWindows() {
        VirtualPath vp = VirtualPath.parse("C:\\", true);

        assertThat(vp.getParentPath()).isEqualTo("C:");
        assertThat(vp.getName()).isEqualTo("");
        assertThat(vp.isDirectory()).isTrue();
        assertThat(vp.toFullPath()).isEqualTo("C:/");
        assertThat(vp.toString()).isEqualTo("C:/");
        assertThat(vp.isAbsolute()).isTrue();
        assertThat(vp.isRelative()).isFalse();

        VirtualPath vp2 = vp.resolve("a", true, null);

        assertThat(vp2.getParentPath()).isEqualTo("C:");
        assertThat(vp2.getName()).isEqualTo("a");
        assertThat(vp2.toFullPath()).isEqualTo("C:/a");

        VirtualPath vp3 = vp2.resolve("b", true, null);

        assertThat(vp3.getParentPath()).isEqualTo("C:/a");
        assertThat(vp3.getName()).isEqualTo("b");
        assertThat(vp3.toFullPath()).isEqualTo("C:/a/b");
    }

    @Test
    public void absoluteWindows() {
        VirtualPath vp = VirtualPath.parse("C:\\a", true);

        assertThat(vp.getParentPath()).isEqualTo("C:");
        assertThat(vp.getName()).isEqualTo("a");
        assertThat(vp.isDirectory()).isTrue();
        assertThat(vp.toFullPath()).isEqualTo("C:/a");
        assertThat(vp.isAbsolute()).isTrue();
        assertThat(vp.isRelative()).isFalse();
    }

    @Test
    public void normalize() {
        VirtualPath vp;

        vp = VirtualPath.parse("a/./b", true, null);

        assertThat(vp.normalize().toFullPath()).isEqualTo("a/b");

        vp = VirtualPath.parse("./b", true, null);

        assertThat(vp.normalize().toFullPath()).isEqualTo("b");

        vp = new VirtualPath(null, "b", true, null);

        assertThat(vp.normalize().toFullPath()).isEqualTo("b");

        vp = VirtualPath.parse("a/../b", true, null);

        assertThat(vp.normalize().toFullPath()).isEqualTo("b");

        /*
        /a/./b/../../c/	/c	. ignored, b popped, a popped.
        //a//b//	/a/b	Multiple slashes (//) collapsed.
                    /../	/	Cannot traverse above root.
        ../a/b	../a/b	Preserves .. in relative paths.
        ./foo	foo	Leading . removed.
        a/b/../c	a/c	b removed by ...
        */
        vp = VirtualPath.parse("/a/./b/../../c/", true, null);

        assertThat(vp.normalize().toFullPath()).isEqualTo("/c");

        vp = VirtualPath.parse("//a//b//", true, null);

        assertThat(vp.normalize().toFullPath()).isEqualTo("/a/b");

        vp = VirtualPath.parse("/../", true, null);

        assertThat(vp.normalize().toFullPath()).isEqualTo("/");

        vp = VirtualPath.parse("../a/b", true, null);

        assertThat(vp.normalize().toFullPath()).isEqualTo("../a/b");

        vp = VirtualPath.parse("a/b/../c", true, null);

        assertThat(vp.normalize().toFullPath()).isEqualTo("a/c");
    }

    @Test
    public void normalizeWindows() {
        VirtualPath vp;

        vp = VirtualPath.parse("a\\.\\b", true, null);

        assertThat(vp.normalize().toFullPath()).isEqualTo("a/b");

        vp = VirtualPath.parse(".\\b", true, null);

        assertThat(vp.normalize().toFullPath()).isEqualTo("b");

        vp = new VirtualPath(null, "b", true, null);

        assertThat(vp.normalize().toFullPath()).isEqualTo("b");

        vp = VirtualPath.parse("C:/b", true, null);

        assertThat(vp.normalize().toFullPath()).isEqualTo("C:/b");
    }

}