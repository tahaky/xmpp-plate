package com.xmpp.plate.util;

import java.security.SecureRandom;

/**
 * Utility class for generating secure passwords
 */
public class PasswordGenerator {

    private static final String CHARACTERS = 
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final int DEFAULT_PASSWORD_LENGTH = 16;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a random secure password
     */
    public static String generatePassword() {
        return generatePassword(DEFAULT_PASSWORD_LENGTH);
    }

    /**
     * Generates a random secure password with specified length
     */
    public static String generatePassword(int length) {
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHARACTERS.length());
            password.append(CHARACTERS.charAt(index));
        }
        return password.toString();
    }
}
