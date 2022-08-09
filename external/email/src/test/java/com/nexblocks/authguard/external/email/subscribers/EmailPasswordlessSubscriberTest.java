package com.nexblocks.authguard.external.email.subscribers;

import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.basic.passwordless.PasswordlessMessageBody;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.external.email.EmailProvider;
import com.nexblocks.authguard.external.email.ImmutableEmail;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AccountEmailBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class EmailPasswordlessSubscriberTest {
    private EmailProvider emailProvider;

    private EmailPasswordlessSubscriber emailPasswordlessSubscriber;

    @BeforeEach
    void setup() {
        emailProvider = Mockito.mock(EmailProvider.class);

        emailPasswordlessSubscriber = new EmailPasswordlessSubscriber(emailProvider);
    }

    @Test
    void onValidMessageWithEmptyTokenOptions() {
        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        final AccountBO account = AccountBO.builder()
                .email(AccountEmailBO.builder()
                        .email("user@test.net")
                        .build())
                .firstName("first")
                .lastName("second")
                .build();

        final TokenOptionsBO tokenOptions = TokenOptionsBO.builder().build();

        final PasswordlessMessageBody messageBody = new PasswordlessMessageBody(accountToken, account, tokenOptions);
        final Message message = Messages.passwordlessGenerated(messageBody);
        final ImmutableEmail expectedEmail = ImmutableEmail.builder()
                .template("passwordless")
                .to(account.getEmail().getEmail())
                .parameters(ImmutableMap.of(
                        "token", accountToken.getToken(),
                        "firstName", account.getFirstName(),
                        "lastName", account.getLastName()))
                .build();

        emailPasswordlessSubscriber.onMessage(message);

        final ArgumentCaptor<ImmutableEmail> sentEmailCaptor = ArgumentCaptor.forClass(ImmutableEmail.class);

        Mockito.verify(emailProvider).send(sentEmailCaptor.capture());

        assertThat(sentEmailCaptor.getValue()).isEqualTo(expectedEmail);
    }

    @Test
    void onValidMessageWithTokenOptions() {
        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        final AccountBO account = AccountBO.builder()
                .email(AccountEmailBO.builder()
                        .email("user@test.net")
                        .build())
                .firstName("first")
                .lastName("second")
                .build();

        final TokenOptionsBO tokenOptions = TokenOptionsBO.builder()
                .sourceIp("127.0.0.1")
                .userAgent("Firefox")
                .build();

        final PasswordlessMessageBody messageBody = new PasswordlessMessageBody(accountToken, account, tokenOptions);
        final Message message = Messages.passwordlessGenerated(messageBody);
        final ImmutableEmail expectedEmail = ImmutableEmail.builder()
                .template("passwordless")
                .to(account.getEmail().getEmail())
                .parameters(ImmutableMap.of(
                        "token", accountToken.getToken(),
                        "firstName", account.getFirstName(),
                        "lastName", account.getLastName(),
                        "sourceIp", tokenOptions.getSourceIp(),
                        "userAgent", tokenOptions.getUserAgent()))
                .build();

        emailPasswordlessSubscriber.onMessage(message);

        final ArgumentCaptor<ImmutableEmail> sentEmailCaptor = ArgumentCaptor.forClass(ImmutableEmail.class);

        Mockito.verify(emailProvider).send(sentEmailCaptor.capture());

        assertThat(sentEmailCaptor.getValue()).isEqualTo(expectedEmail);
    }

    @Test
    void onWrongMessageType() {
        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        final AccountBO account = AccountBO.builder()
                .email(AccountEmailBO.builder()
                        .email("user@test.net")
                        .build())
                .build();

        final PasswordlessMessageBody messageBody = new PasswordlessMessageBody(accountToken, account, null);
        final Message message = Messages.passwordlessGenerated(messageBody)
                .withEventType(EventType.ADMIN);

        emailPasswordlessSubscriber.onMessage(message);

        Mockito.verify(emailProvider, Mockito.never()).send(Mockito.any());
    }

    @Test
    void onValidMessageNoEmail() {
        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        final AccountBO account = AccountBO.builder()
                .build();

        final PasswordlessMessageBody messageBody = new PasswordlessMessageBody(accountToken, account, null);
        final Message message = Messages.passwordlessGenerated(messageBody);

        emailPasswordlessSubscriber.onMessage(message);

        Mockito.verify(emailProvider, Mockito.never()).send(Mockito.any());
    }
}
