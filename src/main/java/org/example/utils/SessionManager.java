package org.example.utils;


import org.example.model.User;

public class SessionManager {
    private static User loggInUser = null;

    public static void login (User user){
        loggInUser = user;
    }
    public static void logout (){
        loggInUser = null;
    }

    public static User getLoggInUser(){
        return loggInUser;
    }

    public static boolean isLoggedIn(){
        return loggInUser != null;
    }
}
