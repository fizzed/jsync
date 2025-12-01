package com.fizzed.jsync.engine;

import com.fizzed.jsync.vfs.VirtualVolume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Paths;
import static com.fizzed.jsync.sftp.SftpVirtualVolume.sftpVolume;
import static com.fizzed.jsync.vfs.LocalVirtualVolume.localVolume;

public class LocalToRemoteDemo {
    static private final Logger log = LoggerFactory.getLogger(LocalToRemoteDemo.class);

    static public void main(String[] args) throws Exception {

        final VirtualVolume source = localVolume(Paths.get("example/source"));
        final VirtualVolume target = sftpVolume("target-host", "remote-dir/target");

        final JsyncResult result = new JsyncEngine()
            .setDelete(true)
            .setParents(true)
            .setForce(true)
            .addIgnore(".git")
            .sync(source, target, JsyncMode.MERGE);
    }

}