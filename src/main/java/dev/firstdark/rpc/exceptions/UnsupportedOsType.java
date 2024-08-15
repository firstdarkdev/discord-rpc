package dev.firstdark.rpc.exceptions;

import dev.firstdark.rpc.enums.OSType;

/**
 * @author HypherionSA
 * Exception thrown when the OS Detector can't detect a valid OS
 */
public class UnsupportedOsType extends Exception {

    private static final long serialVersionUID = 1L;

    public UnsupportedOsType(OSType osType) {
        super("Unsupported Operating System: " + osType.name());
    }

}
