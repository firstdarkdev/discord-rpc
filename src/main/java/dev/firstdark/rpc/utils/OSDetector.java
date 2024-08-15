package dev.firstdark.rpc.utils;

import dev.firstdark.rpc.enums.OSType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author HypherionSA
 * Simple Operating System detector
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OSDetector {

    public static final OSDetector INSTANCE = new OSDetector();

    /**
     * Try to detect what Operating System is currently being used
     *
     * @return The detected OS or {@link OSType#UNKNOWN} if not a known OS
     */
    public OSType detectOs() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win"))
            return OSType.WINDOWS;

        if (osName.contains("mac"))
            return OSType.MAC;

        if (osName.contains("nix") || osName.contains("nux"))
            return OSType.LINUX;

        return OSType.UNKNOWN;
    }
}
