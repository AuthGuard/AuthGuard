package com.nexblocks.authguard.extensions.reset;

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

class ResetTokenSubscriberTest {
    private EmailProvider emailProvider;

    private ResetTokenSubscriber resetTokenSubscriber;

    @BeforeEach
    void setup() {
        emailProvider = Mockito.mock(EmailProvider.class);

        resetTokenSubscriber = new ResetTokenSubscriber(emailProvider);
    }

    @Test
    void onValidMessage() {
        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        final AccountBO account = AccountBO.builder()
                .email(AccountEmailBO.builder()
                        .email("user@test.net")
                        .build())
                .build();

        final ResetTokenMessage messageBody = new ResetTokenMessage(account, accountToken);
        final Message message = Messages.resetTokenGenerated(messageBody);
        final ImmutableEmail expectedEmail = ImmutableEmail.builder()
                .template("passwordReset")
                .to(account.getEmail().getEmail())
                .parameters(Collections.singletonMap("token", accountToken.getToken()))
                .build();

        resetTokenSubscriber.onMessage(message);

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

        final ResetTokenMessage messageBody = new ResetTokenMessage(account, accountToken);
        final Message message = Messages.passwordlessGenerated(messageBody)
                .withEventType(EventType.ADMIN);

        resetTokenSubscriber.onMessage(message);

        Mockito.verify(emailProvider, Mockito.never()).send(Mockito.any());
    }

    @Test
    void onValidMessageNoEmail() {
        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .token("token")
                .build();
        final AccountBO account = AccountBO.builder()
                .build();

        final ResetTokenMessage messageBody = new ResetTokenMessage(account, accountToken);
        final Message message = Messages.passwordlessGenerated(messageBody);

        resetTokenSubscriber.onMessage(message);

        Mockito.verify(emailProvider, Mockito.never()).send(Mockito.any());
    }
}