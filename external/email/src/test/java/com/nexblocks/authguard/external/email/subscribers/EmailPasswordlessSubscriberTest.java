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
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        AccountBO account = AccountBO.builder()
                .email(AccountEmailBO.builder()
                        .email("user@test.net")
                        .build())
                .firstName("first")
                .lastName("second")
                .build();

        TokenOptionsBO tokenOptions = TokenOptionsBO.builder().build();

        PasswordlessMessageBody messageBody = new PasswordlessMessageBody(accountToken, account, tokenOptions);
        Message message = Messages.passwordlessGenerated(messageBody, account.getDomain());
        ImmutableEmail expectedEmail = ImmutableEmail.builder()
                .template("passwordless")
                .to(account.getEmail().getEmail())
                .parameters(ImmutableMap.of(
                        "token", accountToken.getToken(),
                        "firstName", account.getFirstName(),
                        "lastName", account.getLastName()))
                .build();

        emailPasswordlessSubscriber.onMessage(message);

        ArgumentCaptor<ImmutableEmail> sentEmailCaptor = ArgumentCaptor.forClass(ImmutableEmail.class);

        Mockito.verify(emailProvider).send(sentEmailCaptor.capture());

        assertThat(sentEmailCaptor.getValue()).isEqualTo(expectedEmail);
    }

    @Test
    void onValidMessageWithTokenOptions() {
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
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

        PasswordlessMessageBody messageBody = new PasswordlessMessageBody(accountToken, account, tokenOptions);
        Message message = Messages.passwordlessGenerated(messageBody, account.getDomain());
        ImmutableEmail expectedEmail = ImmutableEmail.builder()
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

        ArgumentCaptor<ImmutableEmail> sentEmailCaptor = ArgumentCaptor.forClass(ImmutableEmail.class);

        Mockito.verify(emailProvider).send(sentEmailCaptor.capture());

        assertThat(sentEmailCaptor.getValue()).isEqualTo(expectedEmail);
    }

    @Test
    void onWrongMessageType() {
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        AccountBO account = AccountBO.builder()
                .email(AccountEmailBO.builder()
                        .email("user@test.net")
                        .build())
                .build();

        PasswordlessMessageBody messageBody = new PasswordlessMessageBody(accountToken, account, null);
        Message message = Messages.passwordlessGenerated(messageBody, account.getDomain())
                .withEventType(EventType.ADMIN);

        emailPasswordlessSubscriber.onMessage(message);

        Mockito.verify(emailProvider, Mockito.never()).send(Mockito.any());
    }

    @Test
    void onValidMessageNoEmail() {
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        AccountBO account = AccountBO.builder()
                .build();

        PasswordlessMessageBody messageBody = new PasswordlessMessageBody(accountToken, account, null);
        Message message = Messages.passwordlessGenerated(messageBody, account.getDomain());

        emailPasswordlessSubscriber.onMessage(message);

        Mockito.verify(emailProvider, Mockito.never()).send(Mockito.any());
    }
}
