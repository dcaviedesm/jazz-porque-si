package com.caviedes.jazz.porque.si.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class OSHelper {
    private static final String OS = System.getProperty("os.name").toLowerCase();
    public static final boolean IS_WINDOWS = (OS.contains("win"));
    public static final boolean IS_MAC = (OS.contains("mac"));
    public static final boolean IS_UNIX = (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"));
    public static final boolean IS_SOLARIS = (OS.contains("sunos"));
}
