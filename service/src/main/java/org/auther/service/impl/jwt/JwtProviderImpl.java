package org.auther.service.impl.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auther.config.ConfigContext;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.auther.service.JTIProvider;
import org.auther.service.JwtProvider;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;

import java.util.Optional;

public class JwtProviderImpl implements JwtProvider {
    private final JTIProvider jtiProvider;
    private final JWTConfig jwtConfig;
    private final TokenGenerator tokenGenerator;

    @Inject
    public JwtProviderImpl(@Named("jwt") final ConfigContext configContext, final JTIProvider jtiProvider) {
        this.jtiProvider = jtiProvider;
        this.jwtConfig = new JWTConfig(configContext);
        this.tokenGenerator = new TokenGenerator(jwtConfig);
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final JWTCreator.Builder tokenBuilder = tokenGenerator.generateUnsignedToken(account);

        if (jwtConfig.useJTI()) {
            tokenBuilder.withJWTId(jtiProvider.next());
        }

        final String token = tokenBuilder.sign(jwtConfig.algorithm());

        final String refreshToken = tokenGenerator.generateUnsignedRefreshToken(account)
                .sign(jwtConfig.algorithm());

        return TokensBO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public Optional<String> validateToken(final String token) {
        return decodeAndVerify(token)
                .map(decodedJWT -> {
                    if (jwtConfig.useJTI()) {
                        if (jtiProvider.validate(decodedJWT.getId())) {
                            return token;
                        } else {
                            return null;
                        }
                    } else {
                        return token;
                    }
                });
    }

    Optional<DecodedJWT> decodeAndVerify(final String token) {
        try {
            return Optional.of(JWT.decode(token))
                    .map(jwtConfig.verifier()::verify);
        } catch (final JWTVerificationException e) {
            return Optional.empty();
        }
    }
}
