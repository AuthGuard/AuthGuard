package com.nexblocks.authguard.extensions;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.persistence.ExchangeAttemptsRepository;
import com.nexblocks.authguard.emb.MessageSubscriber;
import com.nexblocks.authguard.emb.annotations.Channel;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.extensions.config.ImmutableAccountLockerConfig;
import com.nexblocks.authguard.service.AccountLocksService;
import com.nexblocks.authguard.service.messaging.AuthMessage;
import com.nexblocks.authguard.service.model.AccountLockBO;
import com.nexblocks.authguard.service.model.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

@Channel("auth")
public class AccountLocker implements MessageSubscriber {
    private static final Logger LOG = LoggerFactory.getLogger(AccountLocker.class);

    private final ExchangeAttemptsRepository exchangeAttemptsRepository;
    private final AccountLocksService accountLocksService;
    private final ImmutableAccountLockerConfig config;

    @Inject
    public AccountLocker(final ExchangeAttemptsRepository exchangeAttemptsRepository,
                         final AccountLocksService accountLocksService,
                         final @Named("accountLocker") ConfigContext configContext) {
        this(exchangeAttemptsRepository, accountLocksService, configContext.asConfigBean(ImmutableAccountLockerConfig.class));
    }

    public AccountLocker(final ExchangeAttemptsRepository exchangeAttemptsRepository,
                         final AccountLocksService accountLocksService,
                         final ImmutableAccountLockerConfig config) {
        this.exchangeAttemptsRepository = exchangeAttemptsRepository;
        this.accountLocksService = accountLocksService;
        this.config = config;
    }

    @Override
    public void onMessage(final Message message) {
        if (message.getEventType() == EventType.AUTHENTICATION) {

            if (message.getBodyType().equals(AuthMessage.class)) {
                processAuthMessage((AuthMessage) message.getMessageBody());
            } else {
                LOG.warn("A message of type {} was published to the auth channel. Expected {}",
                        message.getBodyType(), AuthMessage.class);
            }

        } else {
            LOG.warn("An event of type {} was published to the authentication channel and cannot be processed",
                    message.getEventType());
        }
    }

    private void processAuthMessage(final AuthMessage authMessage) {
        if (authMessage.getEntityType() == EntityType.ACCOUNT) {
            final Instant now = Instant.now();
            final Instant from = now.minus(Duration.ofMinutes(config.getCheckPeriod()));

            exchangeAttemptsRepository.findByEntityAndTimestamp(authMessage.getEntityId(), from)
                    .thenAccept(attempts -> {
                        final long failedCount = attempts.stream()
                                .filter(attempt -> !attempt.isSuccessful())
                                .count();

                        if (failedCount >= config.getMaxAttempts()) {
                            LOG.info("Account {} had {} failed attempts in the past {} minutes; a lock will be placed",
                                    authMessage.getEntityId(), attempts.size(), config.getCheckPeriod());

                            final AccountLockBO lock = AccountLockBO.builder()
                                    .accountId(authMessage.getEntityId())
                                    .expiresAt(now.plus(Duration.ofMinutes(config.getLockPeriod())))
                                    .build();

                            accountLocksService.create(lock);
                        }
                    });
        } else {
            LOG.info("Skipping entity auth message for entity of type {}", authMessage.getEntityType());
        }
    }
}
