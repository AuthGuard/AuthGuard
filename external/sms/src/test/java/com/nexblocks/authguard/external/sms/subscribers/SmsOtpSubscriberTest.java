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
        OneTimePasswordBO otp = OneTimePasswordBO.builder()
                .password("password")
                .build();

        AccountBO account = AccountBO.builder()
                .phoneNumber(PhoneNumberBO.builder()
                        .number("+178945632")
                        .build())
                .firstName("first")
                .lastName("second")
                .build();

        TokenOptionsBO tokenOptions = TokenOptionsBO.builder()
                .sourceIp("127.0.0.1")
                .userAgent("Firefox")
                .build();

        OtpMessageBody messageBody = new OtpMessageBody(otp, account, tokenOptions,
                false, true);
        Message message = Messages.otpGenerated(messageBody, account.getDomain());
        ImmutableTextMessage expectedEmail = ImmutableTextMessage.builder()
                .to(account.getPhoneNumber().getNumber())
                .template("otp")
                .parameters(ImmutableMap.of(
                        "password", otp.getPassword(),
                        "firstName", account.getFirstName(),
                        "lastName", account.getLastName(),
                        "sourceIp", tokenOptions.getSourceIp(),
                        "userAgent", tokenOptions.getUserAgent()))
                .build();

        otpSubscriber.onMessage(message);

        ArgumentCaptor<ImmutableTextMessage> sentSmsCaptor = ArgumentCaptor.forClass(ImmutableTextMessage.class);

        Mockito.verify(smsProvider).send(sentSmsCaptor.capture());

        assertThat(sentSmsCaptor.getValue()).isEqualTo(expectedEmail);
    }

    @Test
    void onValidMessageWithEmptyTokenOptions() {
        OneTimePasswordBO otp = OneTimePasswordBO.builder()
                .password("password")
                .build();

        AccountBO account = AccountBO.builder()
                .phoneNumber(PhoneNumberBO.builder()
                        .number("+178945632")
                        .build())
                .firstName("first")
                .lastName("second")
                .build();

        TokenOptionsBO tokenOptions = TokenOptionsBO.builder().build();

        OtpMessageBody messageBody = new OtpMessageBody(otp, account, tokenOptions,
                false, true);
        Message message = Messages.otpGenerated(messageBody, account.getDomain());
        ImmutableTextMessage expectedEmail = ImmutableTextMessage.builder()
                .to(account.getPhoneNumber().getNumber())
                .template("otp")
                .parameters(ImmutableMap.of(
                        "password", otp.getPassword(),
                        "firstName", account.getFirstName(),
                        "lastName", account.getLastName()))
                .build();

        otpSubscriber.onMessage(message);

        ArgumentCaptor<ImmutableTextMessage> sentSmsCaptor = ArgumentCaptor.forClass(ImmutableTextMessage.class);

        Mockito.verify(smsProvider).send(sentSmsCaptor.capture());

        assertThat(sentSmsCaptor.getValue()).isEqualTo(expectedEmail);
    }

    @Test
    void onWrongMessageType() {
        OneTimePasswordBO otp = OneTimePasswordBO.builder()
                .password("password")
                .build();

        AccountBO account = AccountBO.builder()
                .phoneNumber(PhoneNumberBO.builder()
                        .number("+178945632")
                        .build())
                .build();

        OtpMessageBody messageBody = new OtpMessageBody(otp, account, null, true, false);
        Message message = Messages.otpGenerated(messageBody, account.getDomain())
                .withEventType(EventType.ADMIN);

        otpSubscriber.onMessage(message);

        Mockito.verify(smsProvider, Mockito.never()).send(Mockito.any());
    }

    @Test
    void onValidMessageNoPhoneNumber() {
        OneTimePasswordBO otp = OneTimePasswordBO.builder()
                .password("password")
                .build();

        AccountBO account = AccountBO.builder()
                .build();

        OtpMessageBody messageBody = new OtpMessageBody(otp, account, null,true, false);
        Message message = Messages.otpGenerated(messageBody, account.getDomain());

        otpSubscriber.onMessage(message);

        Mockito.verify(smsProvider, Mockito.never()).send(Mockito.any());
    }
}