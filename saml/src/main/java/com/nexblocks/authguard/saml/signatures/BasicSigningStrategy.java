package com.nexblocks.authguard.saml.signatures;

import com.nexblocks.authguard.saml.config.SamlConfiguration;
import com.nexblocks.authguard.saml.exchange.SigningSupport;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.security.credential.Credential;

public class BasicSigningStrategy implements SamlSigningStrategy {
    private final Credential credential;

    public BasicSigningStrategy(final SamlConfiguration configuration) {
        if (configuration.getSignature() == null) {
            throw new ConfigurationException("Missing SAML signature configuration");
        }

        if (configuration.getSignature().getPrivateKey() == null
                || configuration.getSignature().getPublicKey() == null) {
            throw new ConfigurationException("Basic SAML signature requires both public and private keys to be included");
        }

        try {
            this.credential = SigningSupport.loadFromPemKeys(configuration.getSignature().getPublicKey(), configuration.getSignature().getPrivateKey());
        } catch (Exception e) {
            throw new ConfigurationException("Failed to read SAML signature keys", e);
        }
    }

    @Override
    public void signSamlObject(final SignableSAMLObject signable) {
        SigningSupport.signSAMLObject(signable, credential);
    }
}
