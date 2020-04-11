package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.external.email.EmailProvider;
import com.authguard.external.email.ImmutableEmail;
import com.authguard.service.VerificationMessageService;
import com.authguard.service.config.VerificationConfig;
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
class VerificationMessageServiceImplTest {

    private EmailProvider mockEmailProvider;
    private AccountTokensRepository mockAccountTokensRepository;

    private VerificationMessageService verificationMessageService;

    @BeforeAll
    void setup() {
        mockEmailProvider = Mockito.mock(EmailProvider.class);
        mockAccountTokensRepository = Mockito.mock(AccountTokensRepository.class);

        final ConfigContext configContext = Mockito.mock(ConfigContext.class);
        final VerificationConfig verificationConfig = VerificationConfig.builder()
                .verifyEmailUrlTemplate("http://link/${token}")
                .emailVerificationLife("1d")
                .build();

        Mockito.when(configContext.asConfigBean(VerificationConfig.class))
                .thenReturn(verificationConfig);

        verificationMessageService = new VerificationMessageServiceImpl(mockEmailProvider, mockAccountTokensRepository, configContext);
    }

    @Test
    void sendVerificationEmailAllUnverified() {
        final AccountBO account = AccountBO.builder()
                .id("account-id")
                .emails(Arrays.asList(
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

        verificationMessageService.sendVerificationEmail(account);

        final ArgumentCaptor<AccountTokenDO> accountTokenCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);
        final ArgumentCaptor<ImmutableEmail> emailCaptor = ArgumentCaptor.forClass(ImmutableEmail.class);

        Mockito.verify(mockAccountTokensRepository).save(accountTokenCaptor.capture());
        Mockito.verify(mockEmailProvider, Mockito.times(1))
                .send(emailCaptor.capture());

        final AccountTokenDO accountToken = accountTokenCaptor.getValue();
        final ImmutableEmail email = emailCaptor.getValue();

        assertThat(accountToken.getAssociatedAccountId()).isEqualTo(account.getId());
        assertThat(accountToken.getAdditionalInformation()).isEqualTo("unverified");
        assertThat(accountToken.getToken()).isNotNull();
        assertThat(accountToken.getExpiresAt()).isNotNull();

        assertThat(email.getTo()).isEqualTo("unverified");
        assertThat(email.getBody()).isNull();
        assertThat(email.getParameters()).containsOnlyKeys("url");
    }
}