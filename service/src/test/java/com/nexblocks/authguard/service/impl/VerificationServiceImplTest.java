package com.nexblocks.authguard.service.impl;

import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.basic.otp.OtpProvider;
import com.nexblocks.authguard.basic.otp.OtpVerifier;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.external.sms.SmsProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.VerificationService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.model.*;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificationServiceImplTest {
    private AccountsService accountsService;
    private AccountTokensRepository accountTokensRepository;
    private SmsProvider smsProvider;
    private OtpProvider otpProvider;
    private OtpVerifier otpVerifier;
    private VerificationService verificationService;

    @BeforeEach
    void setup() {
        accountsService = Mockito.mock(AccountsService.class);
        smsProvider = Mockito.mock(SmsProvider.class);
        otpProvider = Mockito.mock(OtpProvider.class);
        otpVerifier = Mockito.mock(OtpVerifier.class);
        accountTokensRepository = Mockito.mock(AccountTokensRepository.class);

        verificationService = new VerificationServiceImpl(accountTokensRepository, accountsService,
                smsProvider, otpProvider, otpVerifier);
    }

    @Test
    void verifyEmail() {
        final AccountBO account = AccountBO.builder()
                .id(101)
                .email(AccountEmailBO.builder()
                        .email("to-verify@test.com")
                        .verified(false)
                        .build())
                .backupEmail(AccountEmailBO.builder()
                        .email("just-backup@test.com")
                        .verified(false)
                        .build())
                .build();

        Mockito.when(accountsService.getById(101, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(account)));

        Mockito.when(accountTokensRepository.getByToken("verification-token"))
                .thenReturn(Uni.createFrom().item(Optional.of(AccountTokenDO.builder()
                        .associatedAccountId(101)
                        .expiresAt(Instant.now().plusSeconds(2))
                                .additionalInformation(ImmutableMap.of("email", "to-verify@test.com"))
                        .build())));

        final ArgumentCaptor<AccountBO> accountCaptor = ArgumentCaptor.forClass(AccountBO.class);

        verificationService.verifyEmail("verification-token", "main");

        Mockito.verify(accountsService).update(accountCaptor.capture(), Mockito.eq("main"));

        assertThat(accountCaptor.getValue()).isNotNull();
        assertThat(accountCaptor.getValue())
                .isEqualTo(account.withEmail(account.getEmail().withVerified(true)));
    }

    @Test
    void verifyEmailWrongEmail() {
        final AccountBO account = AccountBO.builder()
                .id(101)
                .email(AccountEmailBO.builder()
                        .email("to-verify@test.com")
                        .verified(false)
                        .build())
                .backupEmail(AccountEmailBO.builder()
                        .email("just-backup@test.com")
                        .verified(false)
                        .build())
                .build();

        Mockito.when(accountsService.getById(101, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(account)));

        Mockito.when(accountTokensRepository.getByToken("verification-token"))
                .thenReturn(Uni.createFrom().item(Optional.of(AccountTokenDO.builder()
                        .associatedAccountId(101)
                        .expiresAt(Instant.now().plusSeconds(2))
                        .additionalInformation(ImmutableMap.of("email", "wrong@test.com"))
                        .build())));

        assertThatThrownBy(() -> verificationService.verifyEmail("verification-token", "main"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void verifyEmailNoEmail() {
        final AccountBO account = AccountBO.builder()
                .id(101)
                .email(AccountEmailBO.builder()
                        .email("to-verify@test.com")
                        .verified(false)
                        .build())
                .backupEmail(AccountEmailBO.builder()
                        .email("just-backup@test.com")
                        .verified(false)
                        .build())
                .build();

        Mockito.when(accountsService.getById(101, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(account)));

        Mockito.when(accountTokensRepository.getByToken("verification-token"))
                .thenReturn(Uni.createFrom().item(Optional.of(AccountTokenDO.builder()
                        .associatedAccountId(101)
                        .expiresAt(Instant.now().plusSeconds(2))
                        .build())));

        assertThatThrownBy(() -> verificationService.verifyEmail("verification-token", "main"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void verifyEmailExpiredToken() {
        final AccountBO account = AccountBO.builder()
                .id(101)
                .email(AccountEmailBO.builder()
                        .email("to-verify@test.com")
                        .verified(false)
                        .build())
                .backupEmail(AccountEmailBO.builder()
                        .email("just-backup@test.com")
                        .verified(false)
                        .build())
                .build();

        Mockito.when(accountsService.getById(101, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(account)));

        Mockito.when(accountTokensRepository.getByToken("verification-token"))
                .thenReturn(Uni.createFrom().item(Optional.of(AccountTokenDO.builder()
                        .associatedAccountId(101)
                        .expiresAt(Instant.now().minusSeconds(2))
                        .additionalInformation(ImmutableMap.of("email", "wrong@test.com"))
                        .build())));

        assertThatThrownBy(() -> verificationService.verifyEmail("verification-token", "main"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void sendPhoneNumberVerification() {
        final AccountBO account = AccountBO.builder()
                .id(101)
                .phoneNumber(PhoneNumberBO.builder()
                        .number("33334444")
                        .verified(false)
                        .build())
                .build();

        final AuthResponseBO otp = AuthResponseBO.builder()
                .id("otp-id")
                .token("123456")
                .build();

        Mockito.when(accountsService.getById(101, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(account)));

        Mockito.when(otpProvider.generateToken(account))
                .thenReturn(Uni.createFrom().item(otp));

        final AuthResponseBO actual = verificationService.sendPhoneNumberVerification(101, "main");

        assertThat(actual).isEqualTo(otp);
    }

    @Test
    void sendPhoneNumberVerificationByIdentifier() {
        final AccountBO account = AccountBO.builder()
                .id(101)
                .phoneNumber(PhoneNumberBO.builder()
                        .number("33334444")
                        .verified(false)
                        .build())
                .build();

        final AuthResponseBO otp = AuthResponseBO.builder()
                .id("otp-id")
                .token("123456")
                .build();

        Mockito.when(accountsService.getByIdentifier("username", "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(account)));

        Mockito.when(otpProvider.generateToken(account))
                .thenReturn(Uni.createFrom().item(otp));

        final AuthResponseBO actual = verificationService.sendPhoneNumberVerificationByIdentifier("username", "main");

        assertThat(actual).isEqualTo(otp);
    }

    @Test
    void verifyPhoneNumber() {
        AccountBO account = AccountBO.builder()
                .id(101)
                .phoneNumber(PhoneNumberBO.builder()
                        .number("33334444")
                        .verified(false)
                        .build())
                .build();

        AuthRequest request = AuthRequestBO.builder()
                .token("1:123456")
                .build();

        Mockito.when(accountsService.getById(101, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(account)));

        Mockito.when(otpVerifier.verifyAccountTokenAsync(request))
                .thenReturn(Uni.createFrom().item(101L));;

        // TODO account argument captor
        verificationService.verifyPhoneNumber(1, "main", "123456", "33334444");
    }

    @Test
    void verifyPhoneNumberWrongNumber() {
        AccountBO account = AccountBO.builder()
                .id(101)
                .phoneNumber(PhoneNumberBO.builder()
                        .number("33334444")
                        .verified(false)
                        .build())
                .build();

        AuthRequest request = AuthRequestBO.builder()
                .token("1:123456")
                .build();

        Mockito.when(accountsService.getById(101, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(account)));

        Mockito.when(otpVerifier.verifyAccountTokenAsync(request))
                .thenReturn(Uni.createFrom().item(101L));;

        assertThatThrownBy(() -> verificationService.verifyPhoneNumber(1, "main", "123456", "9999999"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void verifyPhoneNumberNullNumber() {
        AccountBO account = AccountBO.builder()
                .id(101)
                .build();

        AuthRequest request = AuthRequestBO.builder()
                .token("1:123456")
                .build();

        Mockito.when(accountsService.getById(101, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(account)));

        Mockito.when(otpVerifier.verifyAccountTokenAsync(request))
                .thenReturn(Uni.createFrom().item(101L));

        assertThatThrownBy(() -> verificationService.verifyPhoneNumber(1, "main", "123456", "9999999"))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void verifyPhoneNumberNonExistingAccount() {
        AuthRequest request = AuthRequestBO.builder()
                .token("1:123456")
                .build();

        Mockito.when(accountsService.getById(101, "main"))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        Mockito.when(otpVerifier.verifyAccountTokenAsync(request))
                .thenReturn(Uni.createFrom().item(101L));;

        assertThatThrownBy(() -> verificationService.verifyPhoneNumber(1, "main", "123456", "9999999"))
                .isInstanceOf(ServiceException.class);
    }
}