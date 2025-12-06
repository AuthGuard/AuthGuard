package com.nexblocks.authguard.saml.signatures;

import com.nexblocks.authguard.saml.config.SamlConfiguration;
import com.nexblocks.authguard.saml.exchange.SigningSupport;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.security.credential.Credential;

public class KeyStoreSigningStrategy implements SamlSigningStrategy {
    private final Credential credential;

    public KeyStoreSigningStrategy(final SamlConfiguration configuration) {
        if (configuration.getSignature() == null) {
            throw new ConfigurationException("Missing SAML signature configuration");
        }

        if (configuration.getSignature().getKeyStorePath() == null) {
            throw new ConfigurationException("KeyStore SAML signature requires a keystore path to be included");
        }

        try {
            this.credential = SigningSupport.loadSigningCredential(configuration.getSignature().getKeyStorePath(),
                    configuration.getSignature().getKeyStoreKeyAlias(),
                    configuration.getSignature().getKeyStorePassword().toCharArray(),
                    configuration.getSignature().getKeyStorePassword().toCharArray());
        } catch (Exception e) {
            throw new ConfigurationException("Failed to read SAML signature keys", e);
        }
    }

    @Override
    public void signSamlObject(final SignableSAMLObject signable) {
        SigningSupport.signSAMLObject(signable, credential);
    }
}
