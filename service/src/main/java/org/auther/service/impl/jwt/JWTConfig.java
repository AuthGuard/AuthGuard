package org.auther.service.impl.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auther.config.ConfigContext;
import org.auther.service.exceptions.ServiceException;

class JWTConfig {
    private static final String ALGORITHM_CONFIG_PROPERTY = "algorithm";
    private static final String KEY_CONFIG_PROPERTY = "key";
    private static final String ISSUER_CONFIG_PROPERTY = "issuer";
    private static final String JTI_CONFIG_PROPERTY = "strategy.useJTI";

    private final ConfigContext configContext;

    private Algorithm algorithm;
    private JWTVerifier verifier;

    JWTConfig(final ConfigContext configContext) {
        this.configContext = configContext;
    }

    Algorithm algorithm() {
        if (algorithm == null) {
            final String algorithmName = configContext.getAsString(ALGORITHM_CONFIG_PROPERTY);
            final String key = configContext.getAsString(KEY_CONFIG_PROPERTY);

            switch (algorithmName) {
                case "HMAC256":
                    algorithm = Algorithm.HMAC256(key);
                    break;

                case "HMAC512":
                    algorithm = Algorithm.HMAC512(key);
                    break;

                default:
                    throw new ServiceException("Unsupported algorithm " + algorithmName);
            }
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

    boolean useJTI() {
        return configContext.getAsBoolean(JTI_CONFIG_PROPERTY);
    }
}
