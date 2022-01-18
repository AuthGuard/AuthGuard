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
    void onValidMessage() {
        final OneTimePasswordBO otp = OneTimePasswordBO.builder()
                .password("password")
                .build();

        final AccountBO account = AccountBO.builder()
                .email(AccountEmailBO.builder()
                        .email("user@test.net")
                        .build())
                .firstName("first")
                .lastName("second")
                .build();

        final OtpMessageBody messageBody = new OtpMessageBody(otp, account, true, false);
        final Message message = Messages.otpGenerated(messageBody);
        final ImmutableEmail expectedEmail = ImmutableEmail.builder()
                .template("otp")
                .to(account.getEmail().getEmail())
                .parameters(ImmutableMap.of(
                        "password", otp.getPassword(),
                        "firstName", account.getFirstName(),
                        "lastName", account.getLastName()))
                .build();

        otpSubscriber.onMessage(message);

        final ArgumentCaptor<ImmutableEmail> sentEmailCaptor = ArgumentCaptor.forClass(ImmutableEmail.class);

        Mockito.verify(emailProvider).send(sentEmailCaptor.capture());

        assertThat(sentEmailCaptor.getValue()).isEqualTo(expectedEmail);
    }

    @Test
    void onWrongMessageType() {
        final OneTimePasswordBO otp = OneTimePasswordBO.builder()
                .password("password")
                .build();

        final AccountBO account = AccountBO.builder()
                .email(AccountEmailBO.builder()
                        .email("user@test.net")
                        .build())
                .build();

        final OtpMessageBody messageBody = new OtpMessageBody(otp, account, true, false);
        final Message message = Messages.otpGenerated(messageBody)
                .withEventType(EventType.ADMIN);

        otpSubscriber.onMessage(message);

        Mockito.verify(emailProvider, Mockito.never()).send(Mockito.any());
    }

    @Test
    void onValidMessageNoEmail() {
        final OneTimePasswordBO otp = OneTimePasswordBO.builder()
                .password("password")
                .build();

        final AccountBO account = AccountBO.builder()
                .build();

        final OtpMessageBody messageBody = new OtpMessageBody(otp, account, true, false);
        final Message message = Messages.otpGenerated(messageBody);

        otpSubscriber.onMessage(message);

        Mockito.verify(emailProvider, Mockito.never()).send(Mockito.any());
    }
}
