package com.authguard.extensions;

import com.authguard.config.ConfigContext;
import com.authguard.dal.persistence.ExchangeAttemptsRepository;
import com.authguard.emb.MessageSubscriber;
import com.authguard.emb.annotations.Channel;
import com.authguard.emb.model.EventType;
import com.authguard.emb.model.Message;
import com.authguard.extensions.config.ImmutableAccountLockerConfig;
import com.authguard.service.AccountLocksService;
import com.authguard.service.messaging.AuthMessage;
import com.authguard.service.model.AccountLockBO;
import com.authguard.service.model.EntityType;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

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
            final OffsetDateTime now = OffsetDateTime.now();
            final OffsetDateTime from = now.minusMinutes(config.getCheckPeriod());

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
                                    .expiresAt(now.plusMinutes(config.getLockPeriod()))
                                    .build();

                            accountLocksService.create(lock);
                        }
                    });
        } else {
            LOG.info("Skipping entity auth message for entity of type {}", authMessage.getEntityType());
        }
    }
}
