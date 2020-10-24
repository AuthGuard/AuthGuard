package com.authguard.jwt.oauth;

import com.authguard.config.ConfigContext;
import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.auth.AuthProvider;
import com.authguard.service.config.AuthorizationCodeConfig;
import com.authguard.service.config.ConfigParser;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.TokenRestrictionsBO;
import com.authguard.service.model.TokensBO;
import com.authguard.service.random.CryptographicRandom;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.time.ZonedDateTime;
import java.util.UUID;

public class AuthorizationCodeProvider implements AuthProvider {
    private final AccountTokensRepository accountTokensRepository;
    private final ServiceMapper serviceMapper;
    private final AuthorizationCodeConfig config;

    private final CryptographicRandom random;

    @Inject
    public AuthorizationCodeProvider(final AccountTokensRepository accountTokensRepository,
                                     final ServiceMapper serviceMapper,
                                     final @Named("authorizationCode") ConfigContext config) {
        this.accountTokensRepository = accountTokensRepository;
        this.serviceMapper = serviceMapper;
        this.config = config.asConfigBean(AuthorizationCodeConfig.class);

        this.random = new CryptographicRandom();
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
                .expiresAt(ZonedDateTime.now().plus(ConfigParser.parseDuration(config.getLifeTime())))
                .tokenRestrictions(serviceMapper.toDO(restrictions))
                .build();

        accountTokensRepository.save(accountToken);

        return TokensBO.builder()
                .type("authorizationCode")
                .token(code)
                .build();
    }

    @Override
    public TokensBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("Authorization code cannot be generated for applications");
    }
}
