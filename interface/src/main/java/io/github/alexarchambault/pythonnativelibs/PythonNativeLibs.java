package io.github.alexarchambault.pythonnativelibs;

import io.github.alexarchambault.pythonnativelibs.internal.PythonNativeLibsHelper;

import java.util.Map;

public final class PythonNativeLibs {

    public static String[] ldflags() {
        return PythonNativeLibsHelper.ldflags();
    }

    public static String executable() {
        return PythonNativeLibsHelper.executable();
    }

    public static String[] ldflagsNix() {
        return PythonNativeLibsHelper.ldflagsNix();
    }

    public static String[] ldflagsWin() {
        return PythonNativeLibsHelper.ldflagsWin();
    }

    public static String nativeLibrary() {
        return PythonNativeLibsHelper.nativeLibrary();
    }

    public static String[] nativeLibraryPaths() {
        return PythonNativeLibsHelper.nativeLibraryPaths();
    }

    public static Map<String, String> scalapyProperties() {
        return PythonNativeLibsHelper.scalapyProperties();
    }

}
