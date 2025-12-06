package com.nexblocks.authguard.saml.signatures;

import org.opensaml.saml.common.SignableSAMLObject;

public interface SamlSigningStrategy {
    void signSamlObject(SignableSAMLObject signable);
}
