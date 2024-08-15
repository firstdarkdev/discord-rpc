package dev.firstdark.rpc.enums;

/**
 * @author HypherionSA
 * The type of Operating System detected by the SDK
 */
public enum OSType {
    WINDOWS,
    LINUX,
    MAC,
    UNKNOWN;

    /**
     * Shortcut to check if the current OS is Windows
     *
     * @return True if Windows
     */
    public boolean isWindows() {
        return this == WINDOWS;
    }

    /**
     * Shortcut to check if the current OS is Linux
     *
     * @return True if Linux
     */
    public boolean isLinux() {
        return this == LINUX;
    }

    /**
     * Shortcut to check if the current OS is MacOS
     *
     * @return True if MacOS
     */
    public boolean isMac() {
        return this == MAC;
    }

    /**
     * Shortcut to check if the current OS is Unknown
     *
     * @return True if Unknown
     */
    public boolean isUnknown() {
        return this == UNKNOWN;
    }
}
