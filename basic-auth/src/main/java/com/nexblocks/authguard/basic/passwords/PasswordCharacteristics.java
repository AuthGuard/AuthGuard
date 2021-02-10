package com.nexblocks.authguard.basic.passwords;

public class PasswordCharacteristics {
    private boolean containsDigits = false;
    private boolean containsSmallLetters = false;
    private boolean containsCapitalLetters = false;
    private boolean containsSpecialCharacters = false;
    
    public PasswordCharacteristics(final String password) {
        for (final char chr : password.toCharArray()) {
            if (Character.isDigit(chr)) {
                containsDigits = true;
            } else if (Character.isAlphabetic(chr)) {
                if (Character.isLowerCase(chr)) {
                    containsSmallLetters = true;
                } else {
                    containsCapitalLetters = true;
                }
            } else {
                containsSpecialCharacters = true;
            }
        }
    }

    public boolean containsDigits() {
        return containsDigits;
    }

    public boolean containsSmallLetters() {
        return containsSmallLetters;
    }

    public boolean containsCapitalLetters() {
        return containsCapitalLetters;
    }

    public boolean containsSpecialCharacters() {
        return containsSpecialCharacters;
    }
}
