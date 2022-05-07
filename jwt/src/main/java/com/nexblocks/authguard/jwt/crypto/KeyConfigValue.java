package com.nexblocks.authguard.jwt.crypto;

public class KeyConfigValue {
    public boolean isFile;
    public String value;

    public KeyConfigValue(final boolean isFile, final String value) {
        this.isFile = isFile;
        this.value = value;
    }

    public static KeyConfigValue resolveValue(final String value) {
        if (value.startsWith("file:")) {
            final String fileName = value.substring(5);

            if (fileName.isBlank()) {
                throw new IllegalArgumentException("Empty file name. Value: " + value);
            }

            return new KeyConfigValue(true, fileName);
        }

        return new KeyConfigValue(false, value);
    }
}
