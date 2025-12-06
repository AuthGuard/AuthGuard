package com.nexblocks.authguard.saml.signatures;

import com.nexblocks.authguard.saml.config.SamlConfiguration;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;

public class SamlSigner {
    private final SamlSigningStrategy strategy;
    private final boolean signAssertion;
    private final boolean signResponse;

    public SamlSigner(final SamlConfiguration configuration) {
        if (configuration.getSignature() == null) {
            throw new ConfigurationException("Missing SAML signature configuration");
        }

        if (configuration.getSignature().useKeyStore()) {
            this.strategy = new KeyStoreSigningStrategy(configuration);
        } else {
            this.strategy = new BasicSigningStrategy(configuration);
        }
        
        this.signAssertion = configuration.getSignature().signAssertion();
        this.signResponse = configuration.getSignature().signResponse();
    }

    public void signSamlObject(final Assertion assertion, final Response response) {
        if (signAssertion) {
            strategy.signSamlObject(assertion);
        }

        if (signResponse) {
            strategy.signSamlObject(response);
        }
    }

    public void signSamlError(final Response response) {
        strategy.signSamlObject(response);
    }
}
