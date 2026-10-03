package org.example.utils;

public class InputValidator {
    public static boolean isValidString(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    public static boolean isValidPrice(double price) {
        return price >= 0.0;
    }
}