package com.nexblocks.authguard.extensions.verification;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.emb.MessageSubscriber;
import com.nexblocks.authguard.emb.annotations.Channel;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.external.email.EmailProvider;
import com.nexblocks.authguard.external.email.ImmutableEmail;
import com.nexblocks.authguard.service.config.ConfigParser;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.VerificationRequestBO;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.service.util.ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

@Channel("verification")
public class VerificationSubscriber implements MessageSubscriber {
    private final String EMAIL_TEMPLATE = "verify-email";

    private static final Logger LOG = LoggerFactory.getLogger(VerificationSubscriber.class);

    private final EmailProvider emailProvider;
    private final AccountTokensRepository accountTokensRepository;
    private final ImmutableVerificationConfig verificationConfig;

    private final Duration tokenTtl;

    @Inject
    public VerificationSubscriber(final EmailProvider emailProvider,
                                  final AccountTokensRepository accountTokensRepository,
                                  final @Named("verification") ConfigContext verificationConfig) {
        this.emailProvider = emailProvider;
        this.accountTokensRepository = accountTokensRepository;
        this.verificationConfig = verificationConfig.asConfigBean(ImmutableVerificationConfig.class);

        this.tokenTtl = ConfigParser.parseDuration(this.verificationConfig.getEmailVerificationLife());
    }

    @Override
    public void onMessage(final Message message) {
        if (message.getEventType() == EventType.EMAIL_VERIFICATION) {
            if (message.getBodyType() != VerificationRequestBO.class) {
                LOG.error("Received an event of type {} but the body type was invalid", message.getEventType());
            } else {
                sendVerificationEmails((VerificationRequestBO) message.getMessageBody());
            }
        } else {
            LOG.warn("An event of type {} was published to the verification channel and cannot be processed", message.getEventType());
        }
    }

    private void sendVerificationEmails(final VerificationRequestBO verificationRequest) {
        Objects.requireNonNull(verificationRequest.getAccount(), "Account cannot be null");
        Objects.requireNonNull(verificationRequest.getEmails(), "Emails cannot be null");

        if (verificationRequest.getEmails().isEmpty()) {
            LOG.warn("A verification request was published with an empty list of emails");
        } else {
            doSendEmails(verificationRequest);
        }
    }

    private void doSendEmails(final VerificationRequestBO verificationRequest) {
        final AccountBO account = verificationRequest.getAccount();

        verificationRequest.getEmails().forEach(email -> {
            if (email == null) {
                LOG.warn("Email is null. Skipping.");
            } else if (email.isVerified()) {
                LOG.warn("Email is already verified. Skipping.");
            } else {
                final String token = generateVerificationString();

                final AccountTokenDO accountToken = AccountTokenDO.builder()
                        .expiresAt(OffsetDateTime.now().plus(tokenTtl))
                        .associatedAccountId(account.getId())
                        .token(token)
                        .additionalInformation(Collections.singletonMap("email", email.getEmail()))
                        .build();

                accountTokensRepository.save(accountToken);

                final ImmutableEmail email1 = ImmutableEmail.builder()
                        .template(EMAIL_TEMPLATE)
                        .to(email.getEmail())
                        .putParameters("token", token)
                        .build();

                emailProvider.send(email1);

                LOG.info("Sent a verification email");
            }
        });
    }

    private String generateVerificationString() {
        final byte[] verificationCode = ID.generate().getBytes();
        return Base64.getEncoder().encodeToString(verificationCode);
    }
}
