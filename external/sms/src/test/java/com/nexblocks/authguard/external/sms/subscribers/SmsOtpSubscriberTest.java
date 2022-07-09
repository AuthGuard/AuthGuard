package com.nexblocks.authguard.external.sms.subscribers;

import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.basic.otp.OtpMessageBody;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.external.sms.ImmutableTextMessage;
import com.nexblocks.authguard.external.sms.SmsProvider;
import com.nexblocks.authguard.service.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class SmsOtpSubscriberTest {
    private SmsProvider smsProvider;

    private SmsOtpSubscriber otpSubscriber;

    @BeforeEach
    void setup() {
        smsProvider = Mockito.mock(SmsProvider.class);

        otpSubscriber = new SmsOtpSubscriber(smsProvider);
    }

    @Test
    void onValidMessageWithTokenOptions() {
        final OneTimePasswordBO otp = OneTimePasswordBO.builder()
                .password("password")
                .build();

        final AccountBO account = AccountBO.builder()
                .phoneNumber(PhoneNumberBO.builder()
                        .number("+178945632")
                        .build())
                .firstName("first")
                .lastName("second")
                .build();

        final TokenOptionsBO tokenOptions = TokenOptionsBO.builder()
                .sourceIp("127.0.0.1")
                .userAgent("Firefox")
                .build();

        final OtpMessageBody messageBody = new OtpMessageBody(otp, account, tokenOptions,
                false, true);
        final Message message = Messages.otpGenerated(messageBody);
        final ImmutableTextMessage expectedEmail = ImmutableTextMessage.builder()
                .to(account.getPhoneNumber().getNumber())
                .parameters(ImmutableMap.of(
                        "password", otp.getPassword(),
                        "firstName", account.getFirstName(),
                        "lastName", account.getLastName(),
                        "sourceIp", tokenOptions.getSourceIp(),
                        "userAgent", tokenOptions.getUserAgent()))
                .build();

        otpSubscriber.onMessage(message);

        final ArgumentCaptor<ImmutableTextMessage> sentSmsCaptor = ArgumentCaptor.forClass(ImmutableTextMessage.class);

        Mockito.verify(smsProvider).send(sentSmsCaptor.capture());

        assertThat(sentSmsCaptor.getValue()).isEqualTo(expectedEmail);
    }

    @Test
    void onValidMessageWithEmptyTokenOptions() {
        final OneTimePasswordBO otp = OneTimePasswordBO.builder()
                .password("password")
                .build();

        final AccountBO account = AccountBO.builder()
                .phoneNumber(PhoneNumberBO.builder()
                        .number("+178945632")
                        .build())
                .firstName("first")
                .lastName("second")
                .build();

        final TokenOptionsBO tokenOptions = TokenOptionsBO.builder().build();

        final OtpMessageBody messageBody = new OtpMessageBody(otp, account, tokenOptions,
                false, true);
        final Message message = Messages.otpGenerated(messageBody);
        final ImmutableTextMessage expectedEmail = ImmutableTextMessage.builder()
                .to(account.getPhoneNumber().getNumber())
                .parameters(ImmutableMap.of(
                        "password", otp.getPassword(),
                        "firstName", account.getFirstName(),
                        "lastName", account.getLastName()))
                .build();

        otpSubscriber.onMessage(message);

        final ArgumentCaptor<ImmutableTextMessage> sentSmsCaptor = ArgumentCaptor.forClass(ImmutableTextMessage.class);

        Mockito.verify(smsProvider).send(sentSmsCaptor.capture());

        assertThat(sentSmsCaptor.getValue()).isEqualTo(expectedEmail);
    }

    @Test
    void onWrongMessageType() {
        final OneTimePasswordBO otp = OneTimePasswordBO.builder()
                .password("password")
                .build();

        final AccountBO account = AccountBO.builder()
                .phoneNumber(PhoneNumberBO.builder()
                        .number("+178945632")
                        .build())
                .build();

        final OtpMessageBody messageBody = new OtpMessageBody(otp, account, null, true, false);
        final Message message = Messages.otpGenerated(messageBody)
                .withEventType(EventType.ADMIN);

        otpSubscriber.onMessage(message);

        Mockito.verify(smsProvider, Mockito.never()).send(Mockito.any());
    }

    @Test
    void onValidMessageNoPhoneNumber() {
        final OneTimePasswordBO otp = OneTimePasswordBO.builder()
                .password("password")
                .build();

        final AccountBO account = AccountBO.builder()
                .build();

        final OtpMessageBody messageBody = new OtpMessageBody(otp, account, null,true, false);
        final Message message = Messages.otpGenerated(messageBody);

        otpSubscriber.onMessage(message);

        Mockito.verify(smsProvider, Mockito.never()).send(Mockito.any());
    }
}