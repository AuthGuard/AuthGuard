package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.config.JacksonConfigContext;
import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.external.email.EmailProvider;
import com.authguard.external.email.ImmutableEmail;
import com.authguard.service.VerificationService;
import com.authguard.service.config.ImmutableVerificationConfig;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AccountEmailBO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VerificationServiceImplTest {

    private EmailProvider mockEmailProvider;
    private AccountTokensRepository mockAccountTokensRepository;

    private VerificationService verificationService;

    @BeforeAll
    void setup() {
        mockEmailProvider = Mockito.mock(EmailProvider.class);
        mockAccountTokensRepository = Mockito.mock(AccountTokensRepository.class);

        final ConfigContext configContext = Mockito.mock(ConfigContext.class);
        final ImmutableVerificationConfig verificationConfig = ImmutableVerificationConfig.builder()
                .verifyEmailUrlTemplate("http://link/${token}")
                .emailVerificationLife("1d")
                .build();

        Mockito.when(configContext.asConfigBean(ImmutableVerificationConfig.class))
                .thenReturn(verificationConfig);

        verificationService = new VerificationServiceImpl(mockEmailProvider, mockAccountTokensRepository, configContext);
    }

    @Test
    void sendVerificationEmailAllUnverified() {
        final AccountBO account = AccountBO.builder()
                .id("account-id")
                .accountEmails(Arrays.asList(
                        AccountEmailBO.builder()
                                .email("verified")
                                .verified(true)
                                .build(),
                        AccountEmailBO.builder()
                                .email("unverified")
                                .verified(false)
                                .build()
                ))
                .build();

        verificationService.sendVerificationEmail(account);

        final ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);
        final ArgumentCaptor<ImmutableEmail> emailCaptor = ArgumentCaptor.forClass(ImmutableEmail.class);

        Mockito.verify(mockAccountTokensRepository).save(accountTokenCaptor.capture());
        Mockito.verify(mockEmailProvider, Mockito.times(1))
                .send(emailCaptor.capture());

        final AccountTokenDO accountToken = accountTokenCaptor.getValue();
        final ImmutableEmail email = emailCaptor.getValue();

        assertThat(accountToken.getAssociatedAccountId()).isEqualTo(account.getId());
        assertThat(accountToken.getToken()).isNotNull();
        assertThat(accountToken.expiresAt()).isNotNull();

        assertThat(email.getTo()).isEqualTo("unverified");
        assertThat(email.getBody()).isNull();
        assertThat(email.getParameters()).containsOnlyKeys("url");
    }
}