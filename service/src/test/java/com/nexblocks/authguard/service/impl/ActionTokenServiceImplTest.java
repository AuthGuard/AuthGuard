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
import com.nexblocks.authguard.service.model.*;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActionTokenServiceImplTest {

    private AccountsService accountsService;
    private BasicAuthProvider basicAuthProvider;
    private OtpProvider otpProvider;
    private OtpVerifier otpVerifier;
    private AccountTokensRepository accountTokensRepository;

    private ActionTokenService actionTokenService;

    private String[] SKIPPED_FIELDS = new String[] { "token" };

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
        AccountBO accountBO = AccountBO.builder()
                .id(101)
                .build();
        AuthResponseBO otpResponse = AuthResponseBO.builder()
                .token("password-id")
                .build();

        Mockito.when(accountsService.getById(101, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountBO)));
        Mockito.when(otpProvider.generateToken(accountBO)).thenReturn(CompletableFuture.completedFuture(otpResponse));

        AuthResponseBO response = actionTokenService.generateOtp(101, "main").join();

        assertThat(response).isEqualTo(otpResponse);
    }

    @Test
    void generateFromBasicAuth() {
        AuthRequestBO authRequest = AuthRequestBO.builder()
                .identifier("username")
                .password("password")
                .build();
        AccountBO account = AccountBO.builder()
                .id(101)
                .build();

        Mockito.when(basicAuthProvider.getAccount(authRequest)).thenReturn(CompletableFuture.completedFuture(account));
        Mockito.when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, AccountTokenDO.class)));

        ActionTokenBO actual = actionTokenService.generateFromBasicAuth(authRequest, "something").join();
        ActionTokenBO expected = ActionTokenBO.builder()
                .accountId(account.getId())
                .validFor(Duration.ofMinutes(5).toSeconds())
                .build();

        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("token")
                .isEqualTo(expected);
        assertThat(actual.getToken()).isNotNull();
    }

    @Test
    void generateFromOtp() {
        AccountBO account = AccountBO.builder()
                .id(101)
                .build();

        AuthRequest request = AuthRequestBO.builder()
                .token("1:otp")
                .build();

        Mockito.when(otpVerifier.verifyAccountTokenAsync(request)).thenReturn(CompletableFuture.completedFuture(account.getId()));
        Mockito.when(accountsService.getById(101, "main"))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, AccountTokenDO.class)));

        ActionTokenBO actual = actionTokenService.generateFromOtp(1, "main", "otp", "something").join();
        ActionTokenBO expected = ActionTokenBO.builder()
                .accountId(account.getId())
                .validFor(Duration.ofMinutes(5).toSeconds())
                .build();

        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("token")
                .isEqualTo(expected);
        assertThat(actual.getToken()).isNotNull();
    }

    @Test
    void verifyToken() {
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .additionalInformation(ImmutableMap.of("action", "something"))
                .build();

        Mockito.when(accountTokensRepository.getByToken("action-token"))
                .thenReturn(Uni.createFrom().item(Optional.of(accountToken)));

        ActionTokenBO actual = actionTokenService.verifyToken("action-token", "something").join();

        assertThat(actual).isNotNull();
    }

    @Test
    void verifyTokenWrongAction() {
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .additionalInformation(ImmutableMap.of("action", "something"))
                .build();

        Mockito.when(accountTokensRepository.getByToken("action-token"))
                .thenReturn(Uni.createFrom().item(Optional.of(accountToken)));

        assertThatThrownBy(() -> actionTokenService.verifyToken("action-token", "else").join())
                .hasCauseInstanceOf(ServiceException.class)
                .cause()
                .extracting(cause -> ((ServiceException) cause).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN.getCode());
    }

    @Test
    void verifyTokenExpired() {
        AccountTokenDO accountToken = AccountTokenDO.builder()
                .expiresAt(Instant.now().minus(Duration.ofMinutes(1)))
                .additionalInformation(ImmutableMap.of("action", "something"))
                .build();

        Mockito.when(accountTokensRepository.getByToken("action-token"))
                .thenReturn(Uni.createFrom().item(Optional.of(accountToken)));

        assertThatThrownBy(() -> actionTokenService.verifyToken("action-token", "something").join())
                .hasCauseInstanceOf(ServiceException.class)
                .cause()
                .extracting(cause -> ((ServiceException) cause).getErrorCode())
                .isEqualTo(ErrorCode.EXPIRED_TOKEN.getCode());
    }

    @Test
    void verifyTokenWrongToken() {
        Mockito.when(accountTokensRepository.getByToken("action-token"))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        assertThatThrownBy(() -> actionTokenService.verifyToken("action-token", "something").join())
                .hasCauseInstanceOf(ServiceException.class)
                .cause()
                .extracting(cause -> ((ServiceException) cause).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_EXPIRED_OR_DOES_NOT_EXIST.getCode());
    }
}