package com.authguard.service.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.inject.Inject;
import com.authguard.service.AuthProvider;
import com.authguard.service.config.ImmutableJwtConfig;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.TokenBuilderBO;
import com.authguard.service.model.TokensBO;


public class ApiTokenProvider implements AuthProvider {
    private final Algorithm algorithm;
    private final JtiProvider jti;

    @Inject
    public ApiTokenProvider(final ImmutableJwtConfig jwtConfig, final JtiProvider jti) {
        this.jti = jti;

        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getKey());
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        throw new UnsupportedOperationException("API keys cannot be generated for an account");

    }

    @Override
    public TokensBO generateToken(final AppBO app) {
        final TokenBuilderBO tokenBuilder = generateApiToken(app);
        final String token = tokenBuilder.getBuilder().sign(algorithm);

        return TokensBO.builder()
                .token(token)
                .build();
    }

    private TokenBuilderBO generateApiToken(final AppBO app) {
        final String keyId = jti.next();

        final JWTCreator.Builder jwtBuilder = JWT.create()
                .withSubject(app.getId())
                .withJWTId(keyId)
                .withClaim("type", "API");

        return TokenBuilderBO.builder()
                .id(keyId)
                .builder(jwtBuilder)
                .build();
    }
}
