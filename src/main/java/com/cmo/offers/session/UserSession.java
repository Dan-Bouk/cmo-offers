package com.cmo.offers.session;

import com.cmo.offers.entity.UserEntity;

public final class UserSession {

    private static UserEntity currentUser;

    private UserSession() {
        // Prevent instantiation
    }

    public static UserEntity getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(UserEntity user) {
        currentUser = user;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void logout() {
        currentUser = null;
    }
}
