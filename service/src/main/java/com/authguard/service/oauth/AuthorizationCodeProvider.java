package com.authguard.service.oauth;

import com.authguard.config.ConfigContext;
import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.AuthProvider;
import com.authguard.service.config.AuthorizationCodeConfig;
import com.authguard.service.config.ConfigParser;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.TokenRestrictionsBO;
import com.authguard.service.model.TokensBO;
import com.authguard.service.random.CryptographicRandom;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.time.ZonedDateTime;

public class AuthorizationCodeProvider implements AuthProvider {
    private final AccountTokensRepository accountTokensRepository;
    private final AuthorizationCodeConfig config;

    private final CryptographicRandom random;

    @Inject
    public AuthorizationCodeProvider(final AccountTokensRepository accountTokensRepository,
                                     final @Named("authorizationCode") ConfigContext config) {
        this.accountTokensRepository = accountTokensRepository;
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
                .token(code)
                .associatedAccountId(account.getId())
                .expiresAt(ZonedDateTime.now().plus(ConfigParser.parseDuration(config.getLifeTime())))
                .additionalInformation(restrictions)
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
