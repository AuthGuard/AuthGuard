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
    void onValidMessage() {
        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        final AccountBO account = AccountBO.builder()
                .phoneNumber(PhoneNumberBO.builder()
                        .number("+178945632")
                        .build())
                .firstName("first")
                .lastName("second")
                .build();

        final PasswordlessMessageBody messageBody = new PasswordlessMessageBody(accountToken, account);
        final Message message = Messages.passwordlessGenerated(messageBody);
        final ImmutableTextMessage expectedSms = ImmutableTextMessage.builder()
                .to(account.getPhoneNumber().getNumber())
                .parameters(ImmutableMap.of(
                        "token", accountToken.getToken(),
                        "firstName", account.getFirstName(),
                        "lastName", account.getLastName()))
                .build();

        smsPasswordlessSubscriber.onMessage(message);

        final ArgumentCaptor<ImmutableTextMessage> sentSmsCaptor = ArgumentCaptor.forClass(ImmutableTextMessage.class);

        Mockito.verify(smsProvider).send(sentSmsCaptor.capture());

        assertThat(sentSmsCaptor.getValue()).isEqualTo(expectedSms);
    }

    @Test
    void onWrongMessageType() {
        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        final AccountBO account = AccountBO.builder()
                .phoneNumber(PhoneNumberBO.builder()
                        .number("+178945632")
                        .build())
                .build();

        final PasswordlessMessageBody messageBody = new PasswordlessMessageBody(accountToken, account);
        final Message message = Messages.passwordlessGenerated(messageBody)
                .withEventType(EventType.ADMIN);

        smsPasswordlessSubscriber.onMessage(message);

        Mockito.verify(smsProvider, Mockito.never()).send(Mockito.any());
    }

    @Test
    void onValidMessageNoPhoneNumber() {
        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        final AccountBO account = AccountBO.builder()
                .build();

        final PasswordlessMessageBody messageBody = new PasswordlessMessageBody(accountToken, account);
        final Message message = Messages.passwordlessGenerated(messageBody);

        smsPasswordlessSubscriber.onMessage(message);

        Mockito.verify(smsProvider, Mockito.never()).send(Mockito.any());
    }
}