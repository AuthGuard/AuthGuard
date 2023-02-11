package com.nexblocks.authguard.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.auth.ProvidesToken;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.config.StrategyConfig;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.EntityType;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@ProvidesToken("jwtApiKey")
public class JwtApiKeyProvider implements AuthProvider {
    private final String TOKEN_TYPE = "jwt_api_key";

    private final Algorithm algorithm;
    private final JtiProvider jti;
    private final StrategyConfig strategyConfig;

    @Inject
    public JwtApiKeyProvider(final JwtConfig jwtConfig, final JtiProvider jti,
                             final @Named("jwtApiKey") ConfigContext apiKeyConfigContext) {
        this(jwtConfig, jti, apiKeyConfigContext.asConfigBean(StrategyConfig.class));
    }

    public JwtApiKeyProvider(final JwtConfig jwtConfig, final JtiProvider jti,
                             final StrategyConfig strategyConfig) {
        this.jti = jti;
        this.strategyConfig = strategyConfig;

        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getPublicKey(),
                jwtConfig.getPrivateKey());
    }

    @Override
    public AuthResponseBO generateToken(final AccountBO account) {
        throw new UnsupportedOperationException("API keys cannot be generated for an account");
    }

    @Override
    public AuthResponseBO generateToken(final AppBO app) {
        final JwtTokenBuilder tokenBuilder = generateApiToken(app, null);
        final String token = tokenBuilder.getBuilder().sign(algorithm);

        return AuthResponseBO.builder()
                .type(TOKEN_TYPE)
                .token(token)
                .entityType(EntityType.APPLICATION)
                .entityId(app.getId())
                .build();
    }

    @Override
    public AuthResponseBO generateToken(final AppBO app, final Instant expiresAt) {
        final JwtTokenBuilder tokenBuilder = generateApiToken(app, expiresAt);
        final String token = tokenBuilder.getBuilder().sign(algorithm);

        return AuthResponseBO.builder()
                .type(TOKEN_TYPE)
                .token(token)
                .entityType(EntityType.APPLICATION)
                .entityId(app.getId())
                .build();
    }

    private JwtTokenBuilder generateApiToken(final AppBO app, final Instant expiresAt) {
        final String keyId = jti.next();

        final JWTCreator.Builder jwtBuilder = JWT.create()
                .withSubject(app.getId())
                .withJWTId(keyId)
                .withClaim("type", "API");

        if (expiresAt != null) {
            jwtBuilder.withExpiresAt(Date.from(expiresAt));
        }

        if (strategyConfig.includeRoles() && app.getRoles() != null) {
            jwtBuilder.withArrayClaim("roles", app.getRoles().toArray(new String[0]));
        }

        if (strategyConfig.includePermissions() && app.getPermissions() != null) {
            final String[] permissions = app.getPermissions()
                    .stream()
                    .map(JwtPermissionsMapper::permissionToString)
                    .toArray(String[]::new);

            jwtBuilder.withArrayClaim("permissions", permissions);
        }

        if (strategyConfig.includeExternalId() && app.getExternalId() != null) {
            jwtBuilder.withClaim("eid", app.getExternalId());
        }

        return JwtTokenBuilder.builder()
                .id(keyId)
                .builder(jwtBuilder)
                .build();
    }
}
