package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.jwt.crypto.TokenEncryptorAdapter;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.auth.ProvidesToken;
import com.nexblocks.authguard.service.config.ConfigParser;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.config.StrategyConfig;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@ProvidesToken("idToken")
public class IdTokenProvider implements AuthProvider {
    private static final String TOKEN_TYPE = "idToken";

    private final Algorithm algorithm;
    private final JwtGenerator jwtGenerator;
    private final TokenEncryptorAdapter tokenEncryptor;
    private final StrategyConfig strategy;
    private final Duration tokenTtl;
    private final boolean encrypt;

    @Inject
    public IdTokenProvider(final @Named("jwt") ConfigContext jwtConfigContext,
                           final @Named("idToken") ConfigContext idTokenConfigContext,
                           final TokenEncryptorAdapter tokenEncryptor) {
        this(jwtConfigContext.asConfigBean(JwtConfig.class),
                idTokenConfigContext.asConfigBean(StrategyConfig.class),
                tokenEncryptor);
    }

    public IdTokenProvider(final JwtConfig jwtConfig,
                           final StrategyConfig idTokenConfig,
                           final TokenEncryptorAdapter tokenEncryptor) {
        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getPublicKey(), jwtConfig.getPrivateKey());

        this.tokenEncryptor = tokenEncryptor;
        this.strategy = idTokenConfig;
        this.jwtGenerator = new JwtGenerator(jwtConfig);
        this.tokenTtl = ConfigParser.parseDuration(strategy.getTokenLife());
        this.encrypt = jwtConfig.getEncryption() != null;
    }

    @Override
    public CompletableFuture<AuthResponseBO> generateToken(final AccountBO account, final TokenRestrictionsBO restrictions,
                                                           final TokenOptionsBO options) {
        if (!account.isActive()) {
            throw new ServiceAuthorizationException(ErrorCode.ACCOUNT_INACTIVE, "Account was deactivated");
        }

        final JwtTokenBuilder tokenBuilder = generateIdToke(account);

        final String signedToken = tokenBuilder.getBuilder().sign(algorithm);
        final String finalToken = encryptIfNeeded(signedToken);

        final String refreshToken = jwtGenerator.generateRandomRefreshToken();

        return CompletableFuture.completedFuture(AuthResponseBO.builder()
                .type(TOKEN_TYPE)
                .token(finalToken)
                .refreshToken(refreshToken)
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .validFor(tokenTtl.getSeconds())
                .build());
    }

    @Override
    public AuthResponseBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("ID tokens cannot be generated for an application");
    }

    private JwtTokenBuilder generateIdToke(final AccountBO account) {
        final JWTCreator.Builder jwtBuilder = jwtGenerator.generateUnsignedToken(account, tokenTtl);

        if (account.getExternalId() != null) {
            jwtBuilder.withClaim("eid", account.getExternalId());
        }

        return JwtTokenBuilder.builder()
                .builder(jwtBuilder)
                .build();
    }

    private String encryptIfNeeded(final String token) {
        return this.encrypt
                ? tokenEncryptor.encryptAndEncode(token).get()
                : token;
    }
}
