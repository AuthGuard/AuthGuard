package com.authguard.service.passwordless;

import com.authguard.config.ConfigContext;
import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.emb.MessageBus;
import com.authguard.emb.Messages;
import com.authguard.service.auth.AuthProvider;
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
import java.util.UUID;

public class PasswordlessProvider implements AuthProvider {
    private static final String PASSWORDLESS_CHANNEL = "passwordless";

    private final AccountTokensRepository accountTokensRepository;
    private final MessageBus messageBus;
    private final PasswordlessConfig passwordlessConfig;
    private final SecureRandom secureRandom;

    @Inject
    public PasswordlessProvider(final AccountTokensRepository accountTokensRepository,
                                final MessageBus messageBus,
                                final @Named("passwordless") ConfigContext configContext) {
        this.accountTokensRepository = accountTokensRepository;
        this.messageBus = messageBus;
        this.passwordlessConfig = configContext.asConfigBean(PasswordlessConfig.class);

        this.secureRandom = new SecureRandom();
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final String token = randomToken();

        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .id(UUID.randomUUID().toString())
                .associatedAccountId(account.getId())
                .token(token)
                .expiresAt(ZonedDateTime.now().plus(ConfigParser.parseDuration(passwordlessConfig.getTokenLife())))
                .build();

        accountTokensRepository.save(accountToken);

        messageBus.publish(PASSWORDLESS_CHANNEL, Messages.passwordlessGenerated(accountToken));

        return TokensBO.builder()
                .type("passwordless")
                .token(accountToken.getId())
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
