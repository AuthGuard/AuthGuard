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
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            final PasswordlessMessageBody body = (PasswordlessMessageBody) message.getMessageBody();

            sendSms(body.getAccount(), body.getAccountToken(), body.getTokenOptions());
        }
    }

    private void sendSms(final AccountBO account, final AccountTokenDO accountToken,
                         final TokenOptionsBO tokenOptions) {
        if (account.getPhoneNumber() != null) {
            final ImmutableMap.Builder<String, String> parameters
                    = SmsParametersHelper.getForAccount(account, tokenOptions);

            parameters.put("token", accountToken.getToken());

            final ImmutableTextMessage sms = ImmutableTextMessage.builder()
                    .template("passwordless")
                    .to(account.getPhoneNumber().getNumber())
                    .parameters(parameters.build())
                    .build();

            smsProvider.send(sms);
        } else {
            LOG.error("A passwordless token was generated for an account without a phone number. Account: {}, token ID: {}",
                    account.getId(), accountToken.getId());
        }
    }
}
