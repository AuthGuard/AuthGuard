package com.nexblocks.authguard.external.email.subscribers;

import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.basic.otp.OtpMessageBody;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.external.email.EmailProvider;
import com.nexblocks.authguard.external.email.ImmutableEmail;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AccountEmailBO;
import com.nexblocks.authguard.service.model.OneTimePasswordBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

public class EmailOtpSubscriberTest {
    private EmailProvider emailProvider;

    private EmailOtpSubscriber otpSubscriber;

    @BeforeEach
    void setup() {
        emailProvider = Mockito.mock(EmailProvider.class);

        otpSubscriber = new EmailOtpSubscriber(emailProvider);
    }

    @Test
    void onValidMessageWithTokenOptions() {
        OneTimePasswordBO otp = OneTimePasswordBO.builder()
                .password("password")
                .build();

        AccountBO account = AccountBO.builder()
                .email(AccountEmailBO.builder()
                        .email("user@test.net")
                        .build())
                .firstName("first")
                .lastName("second")
                .build();

        TokenOptionsBO tokenOptions = TokenOptionsBO.builder()
                .sourceIp("127.0.0.1")
                .userAgent("Firefox")
                .build();

        OtpMessageBody messageBody = new OtpMessageBody(otp, account,  tokenOptions,true, false);
        Message message = Messages.otpGenerated(messageBody, account.getDomain());
        ImmutableEmail expectedEmail = ImmutableEmail.builder()
                .template("otp")
                .to(account.getEmail().getEmail())
                .parameters(ImmutableMap.of(
                        "password", otp.getPassword(),
                        "firstName", account.getFirstName(),
                        "lastName", account.getLastName(),
                        "sourceIp", tokenOptions.getSourceIp(),
                        "userAgent", tokenOptions.getUserAgent()))
                .build();

        otpSubscriber.onMessage(message);

        ArgumentCaptor<ImmutableEmail> sentEmailCaptor = ArgumentCaptor.forClass(ImmutableEmail.class);

        Mockito.verify(emailProvider).send(sentEmailCaptor.capture());

        assertThat(sentEmailCaptor.getValue()).isEqualTo(expectedEmail);
    }

    @Test
    void onValidMessageWithEmptyTokenOptions() {
        OneTimePasswordBO otp = OneTimePasswordBO.builder()
                .password("password")
                .build();

        AccountBO account = AccountBO.builder()
                .email(AccountEmailBO.builder()
                        .email("user@test.net")
                        .build())
                .firstName("first")
                .lastName("second")
                .build();

        TokenOptionsBO tokenOptions = TokenOptionsBO.builder().build();

        OtpMessageBody messageBody = new OtpMessageBody(otp, account,  tokenOptions,true, false);
        Message message = Messages.otpGenerated(messageBody, account.getDomain());
        ImmutableEmail expectedEmail = ImmutableEmail.builder()
                .template("otp")
                .to(account.getEmail().getEmail())
                .parameters(ImmutableMap.of(
                        "password", otp.getPassword(),
                        "firstName", account.getFirstName(),
                        "lastName", account.getLastName()))
                .build();

        otpSubscriber.onMessage(message);

        ArgumentCaptor<ImmutableEmail> sentEmailCaptor = ArgumentCaptor.forClass(ImmutableEmail.class);

        Mockito.verify(emailProvider).send(sentEmailCaptor.capture());

        assertThat(sentEmailCaptor.getValue()).isEqualTo(expectedEmail);
    }

    @Test
    void onWrongMessageType() {
        OneTimePasswordBO otp = OneTimePasswordBO.builder()
                .password("password")
                .build();

        AccountBO account = AccountBO.builder()
                .email(AccountEmailBO.builder()
                        .email("user@test.net")
                        .build())
                .build();

        OtpMessageBody messageBody = new OtpMessageBody(otp, account, null,true, false);
        Message message = Messages.otpGenerated(messageBody, account.getDomain())
                .withEventType(EventType.ADMIN);

        otpSubscriber.onMessage(message);

        Mockito.verify(emailProvider, Mockito.never()).send(Mockito.any());
    }

    @Test
    void onValidMessageNoEmail() {
        OneTimePasswordBO otp = OneTimePasswordBO.builder()
                .password("password")
                .build();

        AccountBO account = AccountBO.builder()
                .build();

        OtpMessageBody messageBody = new OtpMessageBody(otp, account, null,true, false);
        Message message = Messages.otpGenerated(messageBody, account.getDomain());

        otpSubscriber.onMessage(message);

        Mockito.verify(emailProvider, Mockito.never()).send(Mockito.any());
    }
}
