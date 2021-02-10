package com.nexblocks.authguard.basic.passwordless;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.config.ConfigParser;
import com.nexblocks.authguard.service.config.PasswordlessConfig;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.UUID;

public class PasswordlessProvider implements AuthProvider {
    private static final String PASSWORDLESS_CHANNEL = "passwordless";

    private final AccountTokensRepository accountTokensRepository;
    private final MessageBus messageBus;
    private final PasswordlessConfig passwordlessConfig;
    private final SecureRandom secureRandom;
    private final Duration tokenTtl;

    @Inject
    public PasswordlessProvider(final AccountTokensRepository accountTokensRepository,
                                final MessageBus messageBus,
                                final @Named("passwordless") ConfigContext configContext) {
        this.accountTokensRepository = accountTokensRepository;
        this.messageBus = messageBus;
        this.passwordlessConfig = configContext.asConfigBean(PasswordlessConfig.class);

        this.secureRandom = new SecureRandom();
        this.tokenTtl = ConfigParser.parseDuration(this.passwordlessConfig.getTokenLife());
    }

    @Override
    public TokensBO generateToken(final AccountBO account) {
        final String token = randomToken();

        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .id(UUID.randomUUID().toString())
                .associatedAccountId(account.getId())
                .token(token)
                .expiresAt(ZonedDateTime.now().plus(tokenTtl))
                .build();

        accountTokensRepository.save(accountToken);

        messageBus.publish(PASSWORDLESS_CHANNEL, Messages.passwordlessGenerated(accountToken));

        return TokensBO.builder()
                .type("passwordless")
                .token(accountToken.getId())
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
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
