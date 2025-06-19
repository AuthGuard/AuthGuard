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
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.util.ID;
import io.smallrye.mutiny.Uni;

import java.time.Instant;
import java.util.Date;

@ProvidesToken("jwtApiKey")
public class JwtApiKeyProvider implements AuthProvider {
    private final String TOKEN_TYPE = "jwt_api_key";

    private final Algorithm algorithm;
    private final StrategyConfig strategyConfig;

    @Inject
    public JwtApiKeyProvider(final JwtConfig jwtConfig,
                             final @Named("jwtApiKey") ConfigContext apiKeyConfigContext) {
        this(jwtConfig, apiKeyConfigContext.asConfigBean(StrategyConfig.class));
    }

    public JwtApiKeyProvider(final JwtConfig jwtConfig,
                             final StrategyConfig strategyConfig) {
        this.strategyConfig = strategyConfig;

        this.algorithm = JwtConfigParser.parseAlgorithm(jwtConfig.getAlgorithm(), jwtConfig.getPublicKey(),
                jwtConfig.getPrivateKey());
    }

    @Override
    public Uni<AuthResponseBO> generateToken(final AccountBO account, final TokenRestrictionsBO restrictions,
                                                           final TokenOptionsBO options) {
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
    public AuthResponseBO generateToken(ClientBO client) {
        final JwtTokenBuilder tokenBuilder = generateApiToken(client, null);
        final String token = tokenBuilder.getBuilder().sign(algorithm);

        return AuthResponseBO.builder()
                .type(TOKEN_TYPE)
                .token(token)
                .entityType(EntityType.CLIENT)
                .entityId(client.getId())
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
        final String keyId = ID.generateSimplifiedUuid();

        final JWTCreator.Builder jwtBuilder = JWT.create()
                .withSubject("" + app.getId())
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

    private JwtTokenBuilder generateApiToken(final ClientBO client, final Instant expiresAt) {
        final String keyId = ID.generateSimplifiedUuid();

        final JWTCreator.Builder jwtBuilder = JWT.create()
                .withSubject("" + client.getId())
                .withJWTId(keyId)
                .withClaim("type", "API")
                .withClaim("clientType", client.getClientType().name());

        if (expiresAt != null) {
            jwtBuilder.withExpiresAt(Date.from(expiresAt));
        }

        if (strategyConfig.includeExternalId() && client.getExternalId() != null) {
            jwtBuilder.withClaim("eid", client.getExternalId());
        }

        return JwtTokenBuilder.builder()
                .id(keyId)
                .builder(jwtBuilder)
                .build();
    }
}
