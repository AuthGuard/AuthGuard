package com.nexblocks.authguard.external.email.subscribers;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.nexblocks.authguard.basic.otp.OtpMessageBody;
import com.nexblocks.authguard.emb.MessageSubscriber;
import com.nexblocks.authguard.emb.annotations.Channel;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.external.email.EmailProvider;
import com.nexblocks.authguard.external.email.ImmutableEmail;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.OneTimePasswordBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@Channel("otp")
public class EmailOtpSubscriber implements MessageSubscriber {
    private static final Logger LOG = LoggerFactory.getLogger(EmailOtpSubscriber.class);

    private final EmailProvider emailProvider;

    @Inject
    public EmailOtpSubscriber(final EmailProvider emailProvider) {
        this.emailProvider = emailProvider;
    }

    @Override
    public void onMessage(final Message message) {
        if (message.getEventType() == EventType.OTP_GENERATED) {
            final OtpMessageBody messageBody = (OtpMessageBody) message.getMessageBody();
            final AccountBO account = messageBody.getAccount();
            final OneTimePasswordBO otp = messageBody.getOtp();

            if (messageBody.isByEmail()) {
                sendEmail(account, otp);
            } else {
                LOG.warn("Email OTP subscriber is enabled but a OTP event was received not to be sent by email");
            }
        }
    }

    private void sendEmail(final AccountBO account, final OneTimePasswordBO otp) {
        if (account.getEmail() != null) {
            final ImmutableMap.Builder<String, String> parameters = ImmutableMap.builder();

            if (account.getFirstName() != null) {
                parameters.put("firstName", account.getFirstName());
            }

            if (account.getLastName() != null) {
                parameters.put("lastName", account.getLastName());
            }

            final ImmutableEmail email = ImmutableEmail.builder()
                    .template("otp")
                    .parameters(parameters.put("password", otp.getPassword()).build())
                    .to(account.getEmail().getEmail())
                    .build();

            emailProvider.send(email);
        } else {
            LOG.error("An email OTP was generated for an account without an email. Account: {}, password ID: {}",
                    account.getId(), otp.getId());
        }
    }
}
