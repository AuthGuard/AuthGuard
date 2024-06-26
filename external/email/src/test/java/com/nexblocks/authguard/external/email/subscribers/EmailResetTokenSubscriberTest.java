package com.nexblocks.authguard.external.email.subscribers;

import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.external.email.EmailProvider;
import com.nexblocks.authguard.external.email.ImmutableEmail;
import com.nexblocks.authguard.service.messaging.ResetTokenMessage;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AccountEmailBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class EmailResetTokenSubscriberTest {
    private EmailProvider emailProvider;

    private EmailResetTokenSubscriber emailResetTokenSubscriber;

    @BeforeEach
    void setup() {
        emailProvider = Mockito.mock(EmailProvider.class);

        emailResetTokenSubscriber = new EmailResetTokenSubscriber(emailProvider);
    }

    @Test
    void onValidMessage() {
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

        ResetTokenMessage messageBody = new ResetTokenMessage(account, accountToken);
        Message message = Messages.resetTokenGenerated(messageBody, account.getDomain());
        ImmutableEmail expectedEmail = ImmutableEmail.builder()
                .template("passwordReset")
                .to(account.getEmail().getEmail())
                .parameters(ImmutableMap.of(
                        "token", accountToken.getToken(),
                        "firstName", account.getFirstName(),
                        "lastName", account.getLastName()))
                .build();

        emailResetTokenSubscriber.onMessage(message);

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

        ResetTokenMessage messageBody = new ResetTokenMessage(account, accountToken);
        Message message = Messages.passwordlessGenerated(messageBody, account.getDomain())
                .withEventType(EventType.ADMIN);

        emailResetTokenSubscriber.onMessage(message);

        Mockito.verify(emailProvider, Mockito.never()).send(Mockito.any());
    }

    @Test
    void onValidMessageNoEmail() {
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        AccountBO account = AccountBO.builder()
                .build();

        ResetTokenMessage messageBody = new ResetTokenMessage(account, accountToken);
        Message message = Messages.passwordlessGenerated(messageBody, account.getDomain());

        emailResetTokenSubscriber.onMessage(message);

        Mockito.verify(emailProvider, Mockito.never()).send(Mockito.any());
    }
}