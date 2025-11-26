package com.fizzed.jsync.sftp.impl;

import com.jcraft.jsch.SftpATTRS;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicReference;

public class SftpATTRSAccessor {

    static private final AtomicReference<Constructor<SftpATTRS>> SFTP_ATTRS_CONSTRUCTOR_REF = new AtomicReference<>();

    static public SftpATTRS createSftpATTRS() {
        Constructor<SftpATTRS> sftpATTRSConstructor = SFTP_ATTRS_CONSTRUCTOR_REF.get();

        if (sftpATTRSConstructor == null) {
            synchronized (SFTP_ATTRS_CONSTRUCTOR_REF) {
                sftpATTRSConstructor = SFTP_ATTRS_CONSTRUCTOR_REF.get();
                // double lock
                if (sftpATTRSConstructor == null) {
                    try {
                        // SftpATTRS has private access for some fucking reason (no idea why)
                        // can we use reflection instead?
                        // 2. Get the Class object
                        Class<SftpATTRS> clazz = SftpATTRS.class;

                        // 3. Get the specific constructor
                        // Note: Use getDeclaredConstructor(), NOT getConstructor()
                        // getConstructor() only finds PUBLIC constructors.
                        Constructor<SftpATTRS> constructor = clazz.getDeclaredConstructor();

                        // 4. The "Magic" Step: Force access
                        constructor.setAccessible(true);

                        sftpATTRSConstructor = constructor;
                        SFTP_ATTRS_CONSTRUCTOR_REF.set(sftpATTRSConstructor);
                    } catch (Throwable t) {
                        throw new RuntimeException("Unable to use reflection to get SftpATTRS instance", t);
                    }
                }
            }
        }

        try {
            return sftpATTRSConstructor.newInstance();
        } catch (Throwable t) {
            throw new RuntimeException("Unable to create SftpATTRS instance", t);
        }
    }

}