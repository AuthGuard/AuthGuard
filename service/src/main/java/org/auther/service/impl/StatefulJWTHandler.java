package org.auther.service.impl;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.inject.Inject;
import org.auther.service.JTIProvider;
import org.auther.service.JWTProvider;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;

import java.time.LocalDateTime;
import java.util.Optional;

public class StatefulJWTHandler extends AbstractJWTHandler implements JWTProvider {
    private final JTIProvider jtiProvider;

    @Inject
    public StatefulJWTHandler(final Algorithm algorithm, final JWTVerifier verifier, final JTIProvider jtiProvider) {
        super(algorithm, verifier);
        this.jtiProvider = jtiProvider;
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final LocalDateTime now = LocalDateTime.now();

        final String token = generateUnsignedToken(account, now)
                .withJWTId(jtiProvider.next())
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
                .filter(decoded -> jtiProvider.validate(decoded.getId()))
                .map(super::toBuilder)
                .map(jwt -> jwt.withJWTId(jtiProvider.next()))
                .map(jwt -> jwt.sign(super.getAlgorithm()));
    }
}
