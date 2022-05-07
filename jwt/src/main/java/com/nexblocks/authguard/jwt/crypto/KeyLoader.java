package com.nexblocks.authguard.jwt.crypto;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class KeyLoader {
    public static byte[] readPemFileOrValue(final String keyPathOrValue) {
        final KeyConfigValue keyValue = KeyConfigValue.resolveValue(keyPathOrValue);

        if (keyValue.isFile) {
            return KeyLoader.readPemKeyFile(keyValue.value);
        }

        return Base64.getDecoder().decode(keyValue.value);
    }

    public static byte[] readTexFileOrValue(final String keyPathOrValue) {
        final KeyConfigValue keyValue = KeyConfigValue.resolveValue(keyPathOrValue);

        if (keyValue.isFile) {
            return KeyLoader.readTextKeyFile(keyValue.value);
        }

        return Base64.getDecoder().decode(keyValue.value);
    }

    private static byte[] readTextKeyFile(final String filePath) {
        try {
            return Files.readAllBytes(new File(filePath).toPath());
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to read txt file " + filePath, e);
        }
    }

    private static byte[] readPemKeyFile(final String filePath) {
        try {
            return readPemFile(new File(filePath));
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to read PEM file " + filePath, e);
        }
    }

    private static byte[] readPemFile(final File pemFile) throws IOException {
        if (!pemFile.isFile() || !pemFile.exists()) {
            throw new FileNotFoundException("Failed to read file " + pemFile.getAbsolutePath());
        }

        try (PemReader reader = new PemReader(new FileReader(pemFile))) {
            final PemObject pemObject = reader.readPemObject();
            return pemObject.getContent();
        }
    }
}
