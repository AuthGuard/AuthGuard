package org.auther.service.impl.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.inject.Inject;
import org.auther.service.JtiProvider;
import org.auther.service.JwtProvider;
import org.auther.service.JwtStrategy;
import org.auther.service.config.ImmutableJwtConfig;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokenBuilderBO;
import org.auther.service.model.TokensBO;

import java.util.Optional;

public class JwtProviderImpl implements JwtProvider {
    private final JtiProvider jti;
    private final JwtStrategy strategy;

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    @Inject
    public JwtProviderImpl(final ImmutableJwtConfig jwtConfig,
                           final JwtStrategy strategy,
                           final JtiProvider jti) {
        this.jti = jti;
        this.strategy = strategy.configure(jti, new TokenGenerator(jwtConfig));
        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getKey());
        this.verifier = JWT.require(algorithm).build();
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final TokenBuilderBO tokenBuilder = strategy.generateToken(account);
        final String token = tokenBuilder.getBuilder().sign(algorithm);
        final String refreshToken = strategy.generateRefreshToken(account);

        return TokensBO.builder()
                .id(tokenBuilder.getId().orElse(null))
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public Optional<DecodedJWT> validateToken(final String token) {
        return decodeAndVerify(token)
                .map(decoded -> {
                    if (strategy.getConfig().getUseJti()) {
                        return jti.validate(decoded.getId()) ? decoded : null;
                    } else {
                        return decoded;
                    }
                });
    }

    private Optional<DecodedJWT> decodeAndVerify(final String token) {
        try {
            return Optional.of(JWT.decode(token))
                    .map(verifier::verify);
        } catch (final JWTVerificationException e) {
            return Optional.empty();
        }
    }
}
