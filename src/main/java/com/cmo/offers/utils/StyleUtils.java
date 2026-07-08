package com.cmo.offers.utils;

public final class StyleUtils {

    private static final String THEME_CSS = "/com/cmo/offers/resources/theme.css";

    private StyleUtils() {
    }

    public static String themeCss(Class<?> ownerClass) {
        return ownerClass.getResource(THEME_CSS).toExternalForm();
    }
}
