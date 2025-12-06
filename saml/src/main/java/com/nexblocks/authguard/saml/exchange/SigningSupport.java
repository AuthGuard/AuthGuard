package com.nexblocks.authguard.saml.exchange;

import com.nexblocks.authguard.crypto.AsymmetricKeys;
import com.nexblocks.authguard.crypto.KeyLoader;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.impl.SignatureBuilder;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;

import java.io.FileInputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

public class SigningSupport {
    private SigningSupport() {}

    public static Credential loadFromPemKeys(final String publicKeyPath,
                                             final String privateKeyPath) {
        byte[] privateKey = KeyLoader.readPemFileOrValue(privateKeyPath);
        byte[] publicKey = KeyLoader.readPemFileOrValue(publicKeyPath);

        try {
            KeyPair keyPair = AsymmetricKeys.rsaFromBase64Keys(publicKey, privateKey);
            return new BasicCredential(keyPair.getPublic(), keyPair.getPrivate());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    /** Load a persistent signing credential (use a keystore or HSM in prod) */
    public static Credential loadSigningCredential(final String p12Path, final String alias,
                                                   final char[] storePwd, final char[] keyPwd) {
        try (FileInputStream fis = new FileInputStream(p12Path)) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(fis, storePwd);
            PrivateKey key = (PrivateKey) ks.getKey(alias, keyPwd);
            X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
            return new BasicX509Credential(cert, key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load signing credential", e);
        }
    }

    public static void signSAMLObject(final SignableSAMLObject signable, final Credential cred) {
        /* attach ds:Signature and sign (happens in place) */
        // TODO we only support RSA + SHA256 for now
        try {
            Signature signature = new SignatureBuilder().buildObject();
            signature.setSigningCredential(cred);
            signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
            signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

            // Include X.509 cert so SPs can resolve the key (must match your IdP metadata)
            X509KeyInfoGeneratorFactory kif = new X509KeyInfoGeneratorFactory();
            kif.setEmitEntityCertificate(true);
            KeyInfoGenerator kig = kif.newInstance();
            KeyInfo ki = kig.generate(cred);
            signature.setKeyInfo(ki);

            signable.setSignature(signature);
            marshall(signable);
            Signer.signObject(signature);
        } catch (Exception e) {
            throw new RuntimeException("Signing failed", e);
        }
    }

    private static void marshall(final XMLObject obj) throws MarshallingException {
        Marshaller marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(obj);
        if (marshaller == null) {
            throw new IllegalStateException("No marshaller for " + obj.getElementQName());
        }

        //  Marshall to DOM (OpenSAML will mark @ID as of type ID where appropriate)
        marshaller.marshall(obj);
    }
}

