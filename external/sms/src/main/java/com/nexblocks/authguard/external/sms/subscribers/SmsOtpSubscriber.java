package com.nexblocks.authguard.external.sms.subscribers;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.nexblocks.authguard.basic.otp.OtpMessageBody;
import com.nexblocks.authguard.emb.MessageSubscriber;
import com.nexblocks.authguard.emb.annotations.Channel;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.external.sms.ImmutableTextMessage;
import com.nexblocks.authguard.external.sms.SmsProvider;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.OneTimePasswordBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Channel("otp")
public class SmsOtpSubscriber implements MessageSubscriber {
    private static final Logger LOG = LoggerFactory.getLogger(SmsOtpSubscriber.class);

    private final SmsProvider smsProvider;

    @Inject
    public SmsOtpSubscriber(final SmsProvider smsProvider) {
        this.smsProvider = smsProvider;
    }

    @Override
    public void onMessage(final Message message) {
        if (message.getEventType() == EventType.OTP_GENERATED) {
            final OtpMessageBody body = (OtpMessageBody) message.getMessageBody();

            if (body.isBySms()) {
                sendSms(body.getAccount(), body.getOtp(), body.getTokenOptions());
            } else {
                LOG.warn("SMS OTP subscriber is enabled but a OTP event was received not to be sent by SMS");
            }
        }
    }

    private void sendSms(final AccountBO account, final OneTimePasswordBO otp,
                         final TokenOptionsBO tokenOptions) {
        if (account.getPhoneNumber() != null) {
            final ImmutableMap.Builder<String, String> parameters
                    = SmsParametersHelper.getForAccount(account, tokenOptions);

            parameters.put("password", otp.getPassword());

            final ImmutableTextMessage sms = ImmutableTextMessage.builder()
                    .to(account.getPhoneNumber().getNumber())
                    .parameters(parameters.build())
                    .build();

            smsProvider.send(sms);
        } else {
            LOG.error("An email OTP was generated for an account without a phone number. Account: {}, password ID: {}",
                    account.getId(), otp.getId());
        }
    }
}
