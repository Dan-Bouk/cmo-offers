package com.cmo.offers.utils;

public final class StyleUtils {

    private static final String THEME_CSS = "/theme.css";

    private StyleUtils() {
    }

    public static String themeCss(Class<?> ownerClass) {
        return ownerClass.getResource(THEME_CSS).toExternalForm();
    }
}
