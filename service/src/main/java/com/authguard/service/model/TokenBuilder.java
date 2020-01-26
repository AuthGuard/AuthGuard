package com.authguard.service.model;

import com.auth0.jwt.JWTCreator;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * A hack to get around Auth0 JWT implementation
 * where there is no way to get the ID of the
 * token without signing and then decoding it.
 */
@Value.Immutable
@BOStyle
public interface TokenBuilder {
    Optional<String> getId();
    JWTCreator.Builder getBuilder();
}
