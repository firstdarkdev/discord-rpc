package dev.firstdark.rpc.connection;

import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.prefs.Preferences;

@ApiStatus.Internal
class WinRegistry {
    static final int HKEY_CURRENT_USER = 0x80000001;
    private static final int REG_SUCCESS = 0;

    private static final int KEY_ALL_ACCESS = 0xf003f;
    private static final int KEY_READ = 0x20019;
    static final Preferences userRoot = Preferences.userRoot();
    private static final Class<? extends Preferences> userClass = WinRegistry.userRoot.getClass();
    private static final Method regOpenKey;
    private static final Method regCloseKey;
    private static final Method regQueryValueEx;
    private static final Method regCreateKeyEx;
    private static final Method regSetValueEx;

    static {
        try {
            regOpenKey = WinRegistry.userClass.getDeclaredMethod("WindowsRegOpenKey", int.class, byte[].class, int.class);
            regOpenKey.setAccessible(true);
            regCloseKey = WinRegistry.userClass.getDeclaredMethod("WindowsRegCloseKey", int.class);
            regCloseKey.setAccessible(true);
            regQueryValueEx = WinRegistry.userClass.getDeclaredMethod("WindowsRegQueryValueEx", int.class, byte[].class);
            regQueryValueEx.setAccessible(true);
            regCreateKeyEx = WinRegistry.userClass.getDeclaredMethod("WindowsRegCreateKeyEx", int.class, byte[].class);
            regCreateKeyEx.setAccessible(true);
            regSetValueEx = WinRegistry.userClass.getDeclaredMethod("WindowsRegSetValueEx", int.class, byte[].class, byte[].class);
            regSetValueEx.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private WinRegistry() {
    }

    static void createKey(String key) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        int[] ret;

        ret = WinRegistry.createKey(WinRegistry.userRoot, WinRegistry.HKEY_CURRENT_USER, key);
        WinRegistry.regCloseKey.invoke(WinRegistry.userRoot, ret[0]);

        if (ret[1] != REG_SUCCESS)
            throw new IllegalArgumentException("rc=" + ret[1] + "  key=" + key);
    }

    static void writeStringValue(String key, String valueName, String value) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        WinRegistry.writeStringValue(WinRegistry.userRoot, WinRegistry.HKEY_CURRENT_USER, key, valueName, value);
    }


    // =====================

    static String readString() throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        int[] handles = (int[]) WinRegistry.regOpenKey.invoke(WinRegistry.userRoot, WinRegistry.HKEY_CURRENT_USER, toCstr("Software\\\\Valve\\\\Steam"), WinRegistry.KEY_READ);
        if (handles[1] != WinRegistry.REG_SUCCESS)
            return null;

        byte[] valb = (byte[]) WinRegistry.regQueryValueEx.invoke(WinRegistry.userRoot, handles[0], toCstr("SteamExe"));
        WinRegistry.regCloseKey.invoke(WinRegistry.userRoot, handles[0]);
        return valb != null ? new String(valb).trim() : null;
    }

    private static int[] createKey(Preferences root, int hkey, String key) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        return (int[]) WinRegistry.regCreateKeyEx.invoke(root, hkey, toCstr(key));
    }

    private static void writeStringValue(Preferences root, int hkey, String key, String valueName, String value) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        int[] handles = (int[]) WinRegistry.regOpenKey.invoke(root, hkey, toCstr(key), WinRegistry.KEY_ALL_ACCESS);

        WinRegistry.regSetValueEx.invoke(root, handles[0], WinRegistry.toCstr(valueName), WinRegistry.toCstr(value));
        WinRegistry.regCloseKey.invoke(root, handles[0]);
    }

    // utility
    private static byte[] toCstr(String str) {
        byte[] result = new byte[str.length() + 1];

        for (int i = 0; i < str.length(); i++)
            result[i] = (byte) str.charAt(i);

        result[str.length()] = 0;
        return result;
    }
}