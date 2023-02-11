package com.nexblocks.authguard.service.impl;

import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.basic.BasicAuthProvider;
import com.nexblocks.authguard.basic.otp.OtpProvider;
import com.nexblocks.authguard.basic.otp.OtpVerifier;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.ActionTokenService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.ActionTokenBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class ActionTokenServiceImplTest {

    private AccountsService accountsService;
    private BasicAuthProvider basicAuthProvider;
    private OtpProvider otpProvider;
    private OtpVerifier otpVerifier;
    private AccountTokensRepository accountTokensRepository;

    private ActionTokenService actionTokenService;

    @BeforeEach
    void setup() {
        accountsService = Mockito.mock(AccountsService.class);
        basicAuthProvider = Mockito.mock(BasicAuthProvider.class);
        otpProvider = Mockito.mock(OtpProvider.class);
        otpVerifier = Mockito.mock(OtpVerifier.class);
        accountTokensRepository = Mockito.mock(AccountTokensRepository.class);

        actionTokenService = new ActionTokenServiceImpl(accountsService, basicAuthProvider, otpProvider,
                otpVerifier, accountTokensRepository);
    }

    @Test
    void generateOtp() {
        final AccountBO accountBO = AccountBO.builder()
                .id("account")
                .build();
        final AuthResponseBO otpResponse = AuthResponseBO.builder()
                .token("password-id")
                .build();

        Mockito.when(accountsService.getById("account")).thenReturn(Optional.of(accountBO));
        Mockito.when(otpProvider.generateToken(accountBO)).thenReturn(otpResponse);

        final Try<AuthResponseBO> response = actionTokenService.generateOtp("account");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.get()).isEqualTo(otpResponse);
    }

    @Test
    void generateFromBasicAuth() {
        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .identifier("username")
                .password("password")
                .build();
        final AccountBO account = AccountBO.builder()
                .id("account")
                .build();

        Mockito.when(basicAuthProvider.getAccount(authRequest)).thenReturn(Either.right(account));
        Mockito.when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, AccountTokenDO.class)));

        final Try<ActionTokenBO> actual = actionTokenService.generateFromBasicAuth(authRequest, "something");
        final ActionTokenBO expected = ActionTokenBO.builder()
                .accountId(account.getId())
                .validFor(Duration.ofMinutes(5).toSeconds())
                .build();

        assertThat(actual.isSuccess()).isTrue();
        assertThat(actual.get()).isEqualToIgnoringGivenFields(expected, "token");
        assertThat(actual.get().getToken()).isNotNull();
    }

    @Test
    void generateFromOtp() {
        final AccountBO account = AccountBO.builder()
                .id("account")
                .build();

        final String otpToken = "password-id:otp";

        Mockito.when(otpVerifier.verifyAccountToken(otpToken)).thenReturn(Either.right(account.getId()));
        Mockito.when(accountsService.getById("account")).thenReturn(Optional.of(account));
        Mockito.when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, AccountTokenDO.class)));

        final Try<ActionTokenBO> actual = actionTokenService.generateFromOtp("password-id", "otp", "something");
        final ActionTokenBO expected = ActionTokenBO.builder()
                .accountId(account.getId())
                .validFor(Duration.ofMinutes(5).toSeconds())
                .build();

        assertThat(actual.isSuccess()).isTrue();
        assertThat(actual.get()).isEqualToIgnoringGivenFields(expected, "token");
        assertThat(actual.get().getToken()).isNotNull();
    }

    @Test
    void verifyToken() {
        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .additionalInformation(ImmutableMap.of("action", "something"))
                .build();

        Mockito.when(accountTokensRepository.getByToken("action-token"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountToken)));

        final Try<ActionTokenBO> actual = actionTokenService.verifyToken("action-token", "something");

        assertThat(actual.isSuccess()).isTrue();
    }

    @Test
    void verifyTokenWrongAction() {
        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .additionalInformation(ImmutableMap.of("action", "something"))
                .build();

        Mockito.when(accountTokensRepository.getByToken("action-token"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountToken)));

        final Try<ActionTokenBO> actual = actionTokenService.verifyToken("action-token", "else");

        assertThat(actual.isFailure());
        assertThat(((ServiceException) actual.getCause()).getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN.getCode());
    }

    @Test
    void verifyTokenExpired() {
        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .expiresAt(Instant.now().minus(Duration.ofMinutes(1)))
                .additionalInformation(ImmutableMap.of("action", "something"))
                .build();

        Mockito.when(accountTokensRepository.getByToken("action-token"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountToken)));

        final Try<ActionTokenBO> actual = actionTokenService.verifyToken("action-token", "something");

        assertThat(actual.isFailure()).isTrue();
        assertThat(((ServiceException) actual.getCause()).getErrorCode())
                .isEqualTo(ErrorCode.EXPIRED_TOKEN.getCode());
    }

    @Test
    void verifyTokenWrongToken() {
        Mockito.when(accountTokensRepository.getByToken("action-token"))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final Try<ActionTokenBO> actual = actionTokenService.verifyToken("action-token", "something");

        assertThat(actual.isFailure()).isTrue();
        assertThat(((ServiceException) actual.getCause()).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_EXPIRED_OR_DOES_NOT_EXIST.getCode());
    }
}