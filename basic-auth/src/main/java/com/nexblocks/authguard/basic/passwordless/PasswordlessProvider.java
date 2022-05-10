package com.nexblocks.authguard.basic.passwordless;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.basic.config.PasswordlessConfig;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.service.auth.AuthProvider;
import com.nexblocks.authguard.service.auth.ProvidesToken;
import com.nexblocks.authguard.service.config.ConfigParser;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import com.nexblocks.authguard.service.util.ID;

import java.time.Duration;
import java.time.OffsetDateTime;

@ProvidesToken("passwordless")
public class PasswordlessProvider implements AuthProvider {
    private static final String TOKEN_TYPE = "passwordless";
    private static final String PASSWORDLESS_CHANNEL = "passwordless";

    private final AccountTokensRepository accountTokensRepository;
    private final MessageBus messageBus;
    private final PasswordlessConfig passwordlessConfig;
    private final CryptographicRandom cryptographicRandom;
    private final Duration tokenTtl;

    @Inject
    public PasswordlessProvider(final AccountTokensRepository accountTokensRepository,
                                final MessageBus messageBus,
                                final @Named("passwordless") ConfigContext configContext) {
        this.accountTokensRepository = accountTokensRepository;
        this.messageBus = messageBus;
        this.passwordlessConfig = configContext.asConfigBean(PasswordlessConfig.class);

        this.cryptographicRandom = new CryptographicRandom();
        this.tokenTtl = ConfigParser.parseDuration(this.passwordlessConfig.getTokenLife());
    }

    @Override
    public AuthResponseBO generateToken(final AccountBO account) {
        if (!account.isActive()) {
            throw new ServiceAuthorizationException(ErrorCode.ACCOUNT_INACTIVE, "Account was deactivated");
        }

        final String token = randomToken();

        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .id(ID.generate())
                .associatedAccountId(account.getId())
                .token(token)
                .expiresAt(OffsetDateTime.now().plus(tokenTtl))
                .build();

        final AccountTokenDO persistedToken = accountTokensRepository.save(accountToken).join();

        final PasswordlessMessageBody messageBody =
                new PasswordlessMessageBody(persistedToken, account);

        messageBus.publish(PASSWORDLESS_CHANNEL, Messages.passwordlessGenerated(messageBody));

        return AuthResponseBO.builder()
                .type(TOKEN_TYPE)
                .token(persistedToken.getId())
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .build();
    }

    @Override
    public AuthResponseBO generateToken(final AppBO app) {
        throw new UnsupportedOperationException("Passwordless cannot be used for applications");
    }

    private String randomToken() {
        return cryptographicRandom.base64Url(passwordlessConfig.getRandomSize());
    }
}
