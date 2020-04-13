package com.authguard.service.passwordless;

import com.authguard.config.ConfigContext;
import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.AuthProvider;
import com.authguard.service.config.ConfigParser;
import com.authguard.service.config.PasswordlessConfig;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Base64;

public class PasswordlessProvider implements AuthProvider {
    private final AccountTokensRepository accountTokensRepository;
    private final PasswordlessConfig passwordlessConfig;
    private final SecureRandom secureRandom;

    @Inject
    public PasswordlessProvider(final AccountTokensRepository accountTokensRepository,
                                @Named("passwordless") final ConfigContext configContext) {
        this.accountTokensRepository = accountTokensRepository;
        this.passwordlessConfig = configContext.asConfigBean(PasswordlessConfig.class);

        this.secureRandom = new SecureRandom();
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final String token = randomToken();

        accountTokensRepository.save(AccountTokenDO.builder()
                .associatedAccountId(account.getId())
                .token(token)
                .expiresAt(ZonedDateTime.now().plus(ConfigParser.parseDuration(passwordlessConfig.getTokenLife())))
                .build());

        return TokensBO.builder()
                .type("passwordless")
                .build();
    }

    @Override
    public TokensBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("Passwordless cannot be used for applications");
    }

    private String randomToken() {
        final byte[] bytes = new byte[passwordlessConfig.getRandomSize()];

        secureRandom.nextBytes(bytes);

        return Base64.getEncoder().encodeToString(bytes);
    }
}
