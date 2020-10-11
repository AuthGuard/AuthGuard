package com.authguard.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.inject.Inject;
import com.authguard.service.auth.AuthProvider;
import com.authguard.service.config.JwtConfig;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.TokensBO;


public class JwtApiKeyProvider implements AuthProvider {
    private final Algorithm algorithm;
    private final JtiProvider jti;

    @Inject
    public JwtApiKeyProvider(final JwtConfig jwtConfig, final JtiProvider jti) {
        this.jti = jti;

        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getPublicKey(),
                jwtConfig.getPrivateKey());
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        throw new UnsupportedOperationException("API keys cannot be generated for an account");

    }

    @Override
    public TokensBO generateToken(final AppBO app) {
        final JwtTokenBuilder tokenBuilder = generateApiToken(app);
        final String token = tokenBuilder.getBuilder().sign(algorithm);

        return TokensBO.builder()
                .token(token)
                .build();
    }

    private JwtTokenBuilder generateApiToken(final AppBO app) {
        final String keyId = jti.next();

        final JWTCreator.Builder jwtBuilder = JWT.create()
                .withSubject(app.getId())
                .withJWTId(keyId)
                .withClaim("type", "API");

        return JwtTokenBuilder.builder()
                .id(keyId)
                .builder(jwtBuilder)
                .build();
    }
}
