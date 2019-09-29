package org.auther.service.impl.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auther.config.ConfigContext;

import java.time.Duration;

/**
 * A wrapper around a basic configuration which parses JWT
 * config properties.
 */
class JwtConfig {
    private static final String ALGORITHM_CONFIG_PROPERTY = "algorithm";
    private static final String KEY_CONFIG_PROPERTY = "key";
    private static final String ISSUER_CONFIG_PROPERTY = "issuer";
    private static final String JTI_CONFIG_PROPERTY = "strategy.useJti";
    private static final String PERMISSIONS_CONFIG_PROPERTY = "strategy.includePermissions";
    private static final String SCOPES_CONFIG_PROPERTY = "strategy.includeScopes";
    private static final String SIGNED_REFRESH_TOKENS_PROPERTY = "strategy.signedRefreshTokens";
    private static final String TOKEN_LIFE_PROPERTY = "tokenLife";
    private static final String REFRESH_TOKEN_LIFE_PROPERTY = "refreshTokenLife";

    private final ConfigContext configContext;

    private Algorithm algorithm;
    private JWTVerifier verifier;
    private Duration tokenLife;
    private Duration refreshTokenLife;

    JwtConfig(final ConfigContext configContext) {
        this.configContext = configContext;
    }

    Algorithm algorithm() {
        if (algorithm == null) {
            final String algorithmName = configContext.getAsString(ALGORITHM_CONFIG_PROPERTY);
            final String key = configContext.getAsString(KEY_CONFIG_PROPERTY);

            algorithm = JwtConfigParser.parseAlgorithm(algorithmName, key);
        }

        return algorithm;
    }

    JWTVerifier verifier() {
        if (verifier == null) {
            verifier = JWT.require(algorithm()).build();
        }

        return verifier;
    }

    String issuer() {
        return configContext.getAsString(ISSUER_CONFIG_PROPERTY);
    }

    boolean useJti() {
        return configContext.getAsBoolean(JTI_CONFIG_PROPERTY);
    }

    boolean includePermissions() {
        return configContext.getAsBoolean(PERMISSIONS_CONFIG_PROPERTY);
    }

    boolean includeScopes() {
        return configContext.getAsBoolean(SCOPES_CONFIG_PROPERTY);
    }

    boolean signedRefreshTokens() {
        return configContext.getAsBoolean(SIGNED_REFRESH_TOKENS_PROPERTY);
    }

    Duration tokenLife() {
        if (tokenLife == null) {
            tokenLife = JwtConfigParser.parseDuration(configContext.getAsString(TOKEN_LIFE_PROPERTY));
        }

        return tokenLife;
    }

    Duration refreshTokenLife() {
        if (refreshTokenLife == null) {
            refreshTokenLife = JwtConfigParser.parseDuration(configContext.getAsString(REFRESH_TOKEN_LIFE_PROPERTY));
        }

        return refreshTokenLife;
    }
}
