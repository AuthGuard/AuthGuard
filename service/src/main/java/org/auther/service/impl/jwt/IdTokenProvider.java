package org.auther.service.impl.jwt;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.inject.Inject;
import org.auther.service.JtiProvider;
import org.auther.service.JwtProvider;
import org.auther.service.config.ImmutableJwtConfig;
import org.auther.service.config.ImmutableStrategyConfig;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokenBuilderBO;
import org.auther.service.model.TokensBO;

import java.util.Optional;

public class IdTokenProvider implements JwtProvider {
    private final Algorithm algorithm;
    private final TokenGenerator tokenGenerator;
    private final TokenVerifier tokenVerifier;
    private final ImmutableStrategyConfig strategy;

    @Inject
    public IdTokenProvider(final ImmutableJwtConfig jwtConfig) {
        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getKey());
        this.tokenGenerator = new TokenGenerator(jwtConfig);
        this.strategy = jwtConfig.getStrategies().getIdToken();

        this.tokenVerifier = new TokenVerifier(this.strategy, algorithm);
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final TokenBuilderBO tokenBuilder = generateIdToke(account);
        final String token = tokenBuilder.getBuilder().sign(algorithm);
        final String refreshToken = tokenGenerator.generateRandomRefreshToken();

        return TokensBO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public Optional<DecodedJWT> validateToken(final String token) {
        return tokenVerifier.verify(token);
    }

    private TokenBuilderBO generateIdToke(final AccountBO account) {
        final JWTCreator.Builder jwtBuilder = tokenGenerator
                .generateUnsignedToken(account, JwtConfigParser.parseDuration(strategy.getTokenLife()));

        return TokenBuilderBO.builder()
                .builder(jwtBuilder)
                .build();
    }
}
