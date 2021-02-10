package com.nexblocks.authguard.extensions.verification;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.emb.model.EventType;
import com.nexblocks.authguard.emb.model.Message;
import com.nexblocks.authguard.external.email.EmailProvider;
import com.nexblocks.authguard.external.email.ImmutableEmail;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AccountEmailBO;
import com.nexblocks.authguard.service.model.VerificationRequestBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationSubscriberTest {
    private EmailProvider emailProvider;
    private AccountTokensRepository accountTokensRepository;

    private VerificationSubscriber verificationSubscriber;

    @BeforeEach
    void setup() {
        emailProvider = Mockito.mock(EmailProvider.class);
        accountTokensRepository = Mockito.mock(AccountTokensRepository.class);

        final ConfigContext configContext = Mockito.mock(ConfigContext.class);
        final ImmutableVerificationConfig verificationConfig = ImmutableVerificationConfig.builder()
                .emailVerificationLife("1d")
                .build();

        Mockito.when(configContext.asConfigBean(ImmutableVerificationConfig.class))
                .thenReturn(verificationConfig);

        verificationSubscriber = new VerificationSubscriber(emailProvider, accountTokensRepository, configContext);
    }

    @Test
    void onMessage() {
        final AccountBO account = AccountBO.builder()
                .id("account-id")
                .email(AccountEmailBO.builder()
                        .email("unverified")
                        .verified(false)
                        .build())
                .build();

        final VerificationRequestBO verificationRequest = VerificationRequestBO.builder()
                .account(account)
                .emails(Collections.singletonList(account.getEmail()))
                .build();

        final Message<VerificationRequestBO> message = Message.<VerificationRequestBO>builder()
                .eventType(EventType.EMAIL_VERIFICATION)
                .bodyType(VerificationRequestBO.class)
                .messageBody(verificationRequest)
                .build();

        verificationSubscriber.onMessage(message);

        final ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);
        final ArgumentCaptor<ImmutableEmail> emailCaptor = ArgumentCaptor.forClass(ImmutableEmail.class);

        Mockito.verify(accountTokensRepository).save(accountTokenCaptor.capture());
        Mockito.verify(emailProvider, Mockito.times(1))
                .send(emailCaptor.capture());

        final AccountTokenDO accountToken = accountTokenCaptor.getValue();
        final ImmutableEmail email = emailCaptor.getValue();

        assertThat(accountToken.getAssociatedAccountId()).isEqualTo(account.getId());
        assertThat(accountToken.getAdditionalInformation().get("email")).isEqualTo("unverified");
        assertThat(accountToken.getToken()).isNotNull();
        assertThat(accountToken.getExpiresAt()).isNotNull();

        assertThat(email.getTo()).isEqualTo("unverified");
        assertThat(email.getBody()).isNull();
        assertThat(email.getParameters()).containsOnlyKeys("token");
    }

    @Test
    void wrongEventType() {
        final Message<Object> message = Message.builder()
                .eventType(EventType.ADMIN)
                .build();

        verificationSubscriber.onMessage(message);

        Mockito.verifyZeroInteractions(accountTokensRepository, emailProvider);
    }

    @Test
    void wrongEventBodyType() {
        final Message<Object> message = Message.builder()
                .eventType(EventType.EMAIL_VERIFICATION)
                .bodyType(String.class)
                .build();

        verificationSubscriber.onMessage(message);

        Mockito.verifyZeroInteractions(accountTokensRepository, emailProvider);
    }
}