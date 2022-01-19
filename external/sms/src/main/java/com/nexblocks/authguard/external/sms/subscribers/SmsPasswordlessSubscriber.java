package com.nexblocks.authguard.external.sms.subscribers;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.nexblocks.authguard.basic.passwordless.PasswordlessMessageBody;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.emb.MessageSubscriber;
import com.nexblocks.authguard.emb.annotations.Channel;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.external.sms.ImmutableTextMessage;
import com.nexblocks.authguard.external.sms.SmsProvider;
import com.nexblocks.authguard.service.model.AccountBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Channel("passwordless")
public class SmsPasswordlessSubscriber implements MessageSubscriber {
    private static final Logger LOG = LoggerFactory.getLogger(SmsPasswordlessSubscriber.class);

    private final SmsProvider smsProvider;

    @Inject
    public SmsPasswordlessSubscriber(final SmsProvider smsProvider) {
        this.smsProvider = smsProvider;
    }

    @Override
    public void onMessage(final Message message) {
        if (message.getEventType() == EventType.PASSWORDLESS_GENERATED) {
            final PasswordlessMessageBody messageBody = (PasswordlessMessageBody) message.getMessageBody();
            final AccountBO account = messageBody.getAccount();
            final AccountTokenDO accountToken = messageBody.getAccountToken();

            sendEmail(account, accountToken);
        }
    }

    private void sendEmail(final AccountBO account, final AccountTokenDO accountToken) {
        if (account.getPhoneNumber() != null) {
            final ImmutableMap.Builder<String, String> parameters = ImmutableMap.builder();

            if (account.getFirstName() != null) {
                parameters.put("firstName", account.getFirstName());
            }

            if (account.getLastName() != null) {
                parameters.put("lastName", account.getLastName());
            }

            final ImmutableTextMessage sms = ImmutableTextMessage.builder()
                    .to(account.getPhoneNumber().getNumber())
                    .parameters(parameters.put("token", accountToken.getToken()).build())
                    .build();

            smsProvider.send(sms);
        } else {
            LOG.error("A passwordless token was generated for an account without a phone number. Account: {}, token ID: {}",
                    account.getId(), accountToken.getId());
        }
    }
}
