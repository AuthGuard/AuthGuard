package org.auther.service.impl;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import org.auther.service.JWTProvider;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;

import java.time.LocalDateTime;
import java.util.Optional;

public class BasicJWTHandler extends AbstractJWTHandler implements JWTProvider {
    public BasicJWTHandler(final Algorithm algorithm, final JWTVerifier verifier) {
        super(algorithm, verifier);
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final LocalDateTime now = LocalDateTime.now();

        final String token = generateUnsignedToken(account, now)
                .sign(super.getAlgorithm());

        final String refreshToken = generateUnsignedRefreshToken(account, now)
                .sign(super.getAlgorithm());

        return TokensBO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public Optional<String> validateToken(final String token) {
        return decodeAndVerify(token)
                .map(ignored -> token);
    }
}
