package com.fizzed.jsync.engine;

import com.fizzed.jsync.vfs.VirtualVolume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Paths;
import static com.fizzed.jsync.vfs.LocalVirtualVolume.localVolume;

public class LocalToLocalDemo {
    static private final Logger log = LoggerFactory.getLogger(LocalToLocalDemo.class);

    static public void main(String[] args) throws Exception {

        final VirtualVolume source = localVolume(Paths.get("example/source"));
        final VirtualVolume target = localVolume(Paths.get("example/target"));

        final JsyncResult result = new JsyncEngine()
            .setDelete(true)
            .sync(source, target, JsyncMode.MERGE);
    }

}