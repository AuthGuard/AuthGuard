package org.auther.service.impl.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auther.config.ConfigContext;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.auther.service.JtiProvider;
import org.auther.service.JwtProvider;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;

import java.util.Optional;

public class JwtProviderImpl implements JwtProvider {
    private final JtiProvider jti;
    private final JwtConfig jwtConfig;
    private final TokenGenerator tokenGenerator;

    @Inject
    public JwtProviderImpl(@Named("jwt") final ConfigContext configContext, final JtiProvider jti) {
        this.jti = jti;
        this.jwtConfig = new JwtConfig(configContext);
        this.tokenGenerator = new TokenGenerator(jwtConfig, jti);
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final JWTCreator.Builder tokenBuilder = tokenGenerator.generateUnsignedToken(account);

        final String token = tokenBuilder.sign(jwtConfig.algorithm());

        final String refreshToken;
        if (jwtConfig.signedRefreshTokens()) {
            refreshToken = tokenGenerator.generateUnsignedRefreshToken(account)
                    .sign(jwtConfig.algorithm());
        } else {
            refreshToken = tokenGenerator.generateUnsignedRefreshToken();
        }

        return TokensBO.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public Optional<DecodedJWT> validateToken(final String token) {
        return decodeAndVerify(token)
                .map(decoded -> {
                    if (jwtConfig.useJti()) {
                        return jti.validate(decoded.getId()) ? decoded : null;
                    } else {
                        return decoded;
                    }
                });
    }

    private Optional<DecodedJWT> decodeAndVerify(final String token) {
        try {
            return Optional.of(JWT.decode(token))
                    .map(jwtConfig.verifier()::verify);
        } catch (final JWTVerificationException e) {
            return Optional.empty();
        }
    }
}
