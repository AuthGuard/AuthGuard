package com.nexblocks.authguard.jwt.oauth;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.jwt.exchange.PkceParameters;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.auth.ProvidesToken;
import com.nexblocks.authguard.service.config.AuthorizationCodeConfig;
import com.nexblocks.authguard.service.config.ConfigParser;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import com.nexblocks.authguard.service.util.ID;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@ProvidesToken("authorizationCode")
public class AuthorizationCodeProvider implements AuthProvider {
    private final String TOKEN_TYPE = "authorization_code";
    private final AccountTokensRepository accountTokensRepository;
    private final ServiceMapper serviceMapper;
    private final AuthorizationCodeConfig config;

    private final CryptographicRandom random;
    private final Duration tokenTtl;

    @Inject
    public AuthorizationCodeProvider(final AccountTokensRepository accountTokensRepository,
                                     final ServiceMapper serviceMapper,
                                     final @Named("authorizationCode") ConfigContext config) {
        this.accountTokensRepository = accountTokensRepository;
        this.serviceMapper = serviceMapper;
        this.config = config.asConfigBean(AuthorizationCodeConfig.class);

        this.random = new CryptographicRandom();
        this.tokenTtl = ConfigParser.parseDuration(this.config.getLifeTime());
    }

    @Override
    public CompletableFuture<AuthResponseBO> generateToken(final AccountBO account, final TokenRestrictionsBO restrictions,
                                                           final TokenOptionsBO options) {
        String code = random.base64(config.getRandomSize());

        AccountTokenDO.AccountTokenDOBuilder<?, ?> accountToken = AccountTokenDO.builder()
                .token(TOKEN_TYPE)
                .id(ID.generate())
                .token(code)
                .associatedAccountId(account.getId())
                .expiresAt(Instant.now().plus(tokenTtl))
                .tokenRestrictions(serviceMapper.toDO(restrictions));

        if (options != null) {
            accountToken
                    .sourceAuthType(options.getSource())
                    .userAgent(options.getUserAgent())
                    .externalSessionId(options.getExternalSessionId())
                    .deviceId(options.getDeviceId())
                    .clientId(options.getClientId())
                    .sourceIp(options.getSourceIp())
                    .trackingSession(options.getTrackingSession());

            if (options.getExtraParameters() != null
                    && PkceParameters.class.isAssignableFrom(options.getExtraParameters().getClass())) {
                PkceParameters pkceParameters = (PkceParameters) options.getExtraParameters();
                accountToken.additionalInformation(
                        ImmutableMap.of("codeChallenge", pkceParameters.getCodeChallenge(),
                                "codeChallengeMethod", pkceParameters.getCodeChallengeMethod())
                );
            }
        }

        return accountTokensRepository.save(accountToken.build())
                .map(ignored -> AuthResponseBO.builder()
                        .type("authorizationCode")
                        .token(code)
                        .entityType(EntityType.ACCOUNT)
                        .entityId(account.getId())
                        .build())
                .subscribeAsCompletionStage();
    }

    @Override
    public AuthResponseBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("Authorization code cannot be generated for applications");
    }
}
