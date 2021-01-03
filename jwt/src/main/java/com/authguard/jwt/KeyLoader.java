package com.authguard.jwt;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class KeyLoader {

    public static byte[] readPemKeyFile(final String filePath) {
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
