package org.auther.service.impl.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.inject.Inject;
import org.auther.service.JtiProvider;
import org.auther.service.JwtProvider;
import org.auther.service.config.ImmutableJwtConfig;
import org.auther.service.config.ImmutableStrategyConfig;
import org.auther.service.model.AccountBO;
import org.auther.service.model.AppBO;
import org.auther.service.model.TokenBuilderBO;
import org.auther.service.model.TokensBO;

import java.util.Optional;

public class ApiTokenProvider implements JwtProvider {
    private final Algorithm algorithm;
    private final TokenVerifier tokenVerifier;
    private final JtiProvider jti;

    @Inject
    public ApiTokenProvider(final ImmutableJwtConfig jwtConfig, final JtiProvider jti) {
        this.jti = jti;

        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getKey());

        final ImmutableStrategyConfig strategy = ImmutableStrategyConfig.builder().useJti(true).build();

        this.tokenVerifier = new TokenVerifier(strategy, jti, algorithm);
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

    @Override
    public Optional<DecodedJWT> validateToken(final String token) {
        return tokenVerifier.verify(token);
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
