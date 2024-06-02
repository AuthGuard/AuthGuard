package com.nexblocks.authguard.crypto;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Formats {
    public String publicKeyToPcke8(final PublicKey publicKey) {
        try {
            Writer writer = new StringWriter();
            JcaPEMWriter pemWriter = new JcaPEMWriter(writer);

            pemWriter.writeObject(publicKey);

            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String privateKeyToX509(final PrivateKey privateKey) {
        try {
            Writer writer = new StringWriter();
            JcaPEMWriter pemWriter = new JcaPEMWriter(writer);

            pemWriter.writeObject(privateKey);

            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
