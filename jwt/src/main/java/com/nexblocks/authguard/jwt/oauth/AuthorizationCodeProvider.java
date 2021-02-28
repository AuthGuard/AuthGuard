package com.nexblocks.authguard.jwt.oauth;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.auth.ProvidesToken;
import com.nexblocks.authguard.service.config.AuthorizationCodeConfig;
import com.nexblocks.authguard.service.config.ConfigParser;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;

@ProvidesToken("authorizationCode")
public class AuthorizationCodeProvider implements AuthProvider {
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
    public TokensBO generateToken(final AccountBO account) {
        return generateToken(account, null);
    }

    @Override
    public TokensBO generateToken(final AccountBO account, final TokenRestrictionsBO restrictions) {
        final String code = random.base64(config.getRandomSize());

        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .id(UUID.randomUUID().toString())
                .token(code)
                .associatedAccountId(account.getId())
                .expiresAt(ZonedDateTime.now().plus(tokenTtl))
                .tokenRestrictions(serviceMapper.toDO(restrictions))
                .build();

        accountTokensRepository.save(accountToken);

        return TokensBO.builder()
                .type("authorizationCode")
                .token(code)
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .build();
    }

    @Override
    public TokensBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("Authorization code cannot be generated for applications");
    }
}
