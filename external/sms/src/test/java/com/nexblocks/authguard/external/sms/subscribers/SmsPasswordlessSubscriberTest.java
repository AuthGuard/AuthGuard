package com.nexblocks.authguard.external.sms.subscribers;

import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.basic.passwordless.PasswordlessMessageBody;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.external.sms.ImmutableTextMessage;
import com.nexblocks.authguard.external.sms.SmsProvider;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.PhoneNumberBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class SmsPasswordlessSubscriberTest {
    private SmsProvider smsProvider;

    private SmsPasswordlessSubscriber smsPasswordlessSubscriber;

    @BeforeEach
    void setup() {
        smsProvider = Mockito.mock(SmsProvider.class);

        smsPasswordlessSubscriber = new SmsPasswordlessSubscriber(smsProvider);
    }

    @Test
    void onValidMessageWithEmptyTokenOptions() {
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        AccountBO account = AccountBO.builder()
                .phoneNumber(PhoneNumberBO.builder()
                        .number("+178945632")
                        .build())
                .firstName("first")
                .lastName("second")
                .build();

        TokenOptionsBO tokenOptions = TokenOptionsBO.builder().build();

        PasswordlessMessageBody messageBody = new PasswordlessMessageBody(accountToken, account, tokenOptions);
        Message message = Messages.passwordlessGenerated(messageBody, account.getDomain());
        ImmutableTextMessage expectedSms = ImmutableTextMessage.builder()
                .to(account.getPhoneNumber().getNumber())
                .template("passwordless")
                .parameters(ImmutableMap.of(
                        "token", accountToken.getToken(),
                        "firstName", account.getFirstName(),
                        "lastName", account.getLastName()))
                .build();

        smsPasswordlessSubscriber.onMessage(message);

        ArgumentCaptor<ImmutableTextMessage> sentSmsCaptor = ArgumentCaptor.forClass(ImmutableTextMessage.class);

        Mockito.verify(smsProvider).send(sentSmsCaptor.capture());

        assertThat(sentSmsCaptor.getValue()).isEqualTo(expectedSms);
    }

    @Test
    void onValidMessageWithTokenOptions() {
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
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

        PasswordlessMessageBody messageBody = new PasswordlessMessageBody(accountToken, account, tokenOptions);
        Message message = Messages.passwordlessGenerated(messageBody, account.getDomain());
        ImmutableTextMessage expectedSms = ImmutableTextMessage.builder()
                .to(account.getPhoneNumber().getNumber())
                .template("passwordless")
                .parameters(ImmutableMap.of(
                        "token", accountToken.getToken(),
                        "firstName", account.getFirstName(),
                        "lastName", account.getLastName(),
                        "sourceIp", tokenOptions.getSourceIp(),
                        "userAgent", tokenOptions.getUserAgent()))
                .build();

        smsPasswordlessSubscriber.onMessage(message);

        ArgumentCaptor<ImmutableTextMessage> sentSmsCaptor = ArgumentCaptor.forClass(ImmutableTextMessage.class);

        Mockito.verify(smsProvider).send(sentSmsCaptor.capture());

        assertThat(sentSmsCaptor.getValue()).isEqualTo(expectedSms);
    }

    @Test
    void onWrongMessageType() {
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        AccountBO account = AccountBO.builder()
                .phoneNumber(PhoneNumberBO.builder()
                        .number("+178945632")
                        .build())
                .build();

        PasswordlessMessageBody messageBody = new PasswordlessMessageBody(accountToken, account, null);
        Message message = Messages.passwordlessGenerated(messageBody, account.getDomain())
                .withEventType(EventType.ADMIN);

        smsPasswordlessSubscriber.onMessage(message);

        Mockito.verify(smsProvider, Mockito.never()).send(Mockito.any());
    }

    @Test
    void onValidMessageNoPhoneNumber() {
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        AccountBO account = AccountBO.builder()
                .build();

        PasswordlessMessageBody messageBody = new PasswordlessMessageBody(accountToken, account, null);
        Message message = Messages.passwordlessGenerated(messageBody, account.getDomain());

        smsPasswordlessSubscriber.onMessage(message);

        Mockito.verify(smsProvider, Mockito.never()).send(Mockito.any());
    }
}