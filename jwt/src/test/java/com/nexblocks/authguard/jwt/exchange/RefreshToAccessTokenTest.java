package com.nexblocks.authguard.jwt.exchange;

import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.dal.model.TokenRestrictionsDO;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.*;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshToAccessTokenTest {
    private AccountTokensRepository accountTokensRepository;
    private AccountsService accountsService;
    private AccessTokenProvider accessTokenProvider;

    private RefreshToAccessToken refreshToAccessToken;

    @BeforeEach
    void setup() {
        accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        accountsService = Mockito.mock(AccountsService.class);
        accessTokenProvider = Mockito.mock(AccessTokenProvider.class);

        refreshToAccessToken = new RefreshToAccessToken(accountTokensRepository, accountsService,
                accessTokenProvider, JwtConfig.builder().build(), new ServiceMapperImpl());

        Mockito.when(accountTokensRepository.deleteToken(Mockito.any()))
                .thenReturn(Uni.createFrom().item(Optional.of(AccountTokenDO.builder().build())));
    }

    @Test
    void exchange() throws InterruptedException {
        // data
        long accountId = 101;
        String refreshToken = "refresh_token";

        AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(refreshToken)
                .build();

        AccountTokenDO accountToken = AccountTokenDO.builder()
                .token(refreshToken)
                .associatedAccountId(accountId)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .sourceAuthType("basic")
                .build();

        AccountBO account = AccountBO.builder()
                .id(accountId)
                .build();

        AuthResponseBO newTokens = AuthResponseBO.builder()
                .token("new_token")
                .refreshToken("new_refresh_token")
                .build();

        final TokenOptionsBO options = TokenOptionsBO.builder()
                .source("basic")
                .userAgent(authRequest.getUserAgent())
                .sourceIp(authRequest.getSourceIp())
                .clientId(authRequest.getClientId())
                .externalSessionId(authRequest.getExternalSessionId())
                .deviceId(authRequest.getDeviceId())
                .build();

        // mock
        Mockito.when(accountTokensRepository.getByToken(authRequest.getToken()))
                .thenReturn(Uni.createFrom().item(Optional.of(accountToken)));

        Mockito.when(accountsService.getByIdUnchecked(accountId))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));

        Mockito.when(accessTokenProvider.generateToken(account, null, options))
                .thenReturn(CompletableFuture.completedFuture(newTokens));

        // do
        AuthResponseBO actual = refreshToAccessToken.exchange(authRequest).join();

        // assert
        assertThat(actual).isEqualTo(newTokens);

        Thread.sleep(50L); // triggering deleting happens in the background so give it some time
        Mockito.verify(accountTokensRepository).deleteToken(refreshToken);
    }

    @Test
    void exchangeWithRestrictions() throws InterruptedException {
        // data
        long accountId = 101;
        String refreshToken = "refresh_token";
        String restrictionPermission = "permission.read";

        AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(refreshToken)
                .build();

        AccountTokenDO accountToken = AccountTokenDO.builder()
                .token(refreshToken)
                .associatedAccountId(accountId)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .tokenRestrictions(TokenRestrictionsDO.builder()
                        .permissions(Collections.singleton(restrictionPermission))
                        .scopes(Collections.emptySet())
                        .build())
                .sourceAuthType("basic")
                .build();

        AccountBO account = AccountBO.builder()
                .id(accountId)
                .build();

        AuthResponseBO newTokens = AuthResponseBO.builder()
                .token("new_token")
                .refreshToken("new_refresh_token")
                .build();

        TokenRestrictionsBO restrictions = TokenRestrictionsBO.builder()
                .addPermissions(restrictionPermission)
                .scopes(Collections.emptySet())
                .build();

        TokenOptionsBO options = TokenOptionsBO.builder()
                .source("basic")
                .userAgent(authRequest.getUserAgent())
                .sourceIp(authRequest.getSourceIp())
                .clientId(authRequest.getClientId())
                .externalSessionId(authRequest.getExternalSessionId())
                .deviceId(authRequest.getDeviceId())
                .build();

        // mock
        Mockito.when(accountTokensRepository.getByToken(authRequest.getToken()))
                .thenReturn(Uni.createFrom().item(Optional.of(accountToken)));

        Mockito.when(accountsService.getByIdUnchecked(accountId))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));

        Mockito.when(accessTokenProvider.generateToken(account, restrictions, options))
                .thenReturn(CompletableFuture.completedFuture(newTokens));

        // do
        AuthResponseBO actual = refreshToAccessToken.exchange(authRequest).join();

        // assert
        assertThat(actual).isEqualTo(newTokens);

        Thread.sleep(50L); // triggering deleting happens in the background so give it some time
        Mockito.verify(accountTokensRepository).deleteToken(refreshToken);
    }

    @Test
    void exchangeExpiredToken() throws InterruptedException {
        // data
        long accountId = 101;
        String refreshToken = "refresh_token";

        AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(refreshToken)
                .build();

        AccountTokenDO accountToken = AccountTokenDO.builder()
                .token(refreshToken)
                .associatedAccountId(accountId)
                .expiresAt(Instant.now().minus(Duration.ofMinutes(1)))
                .build();

        // mock
        Mockito.when(accountTokensRepository.getByToken(authRequest.getToken()))
                .thenReturn(Uni.createFrom().item(Optional.of(accountToken)));

        // do
        assertThatThrownBy(() -> refreshToAccessToken.exchange(authRequest).join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class);

        Thread.sleep(50L); // triggering deleting happens in the background so give it some time
        Mockito.verify(accountTokensRepository).deleteToken(refreshToken);
    }

    @Test
    void exchangeNoAccount() {
        // data
        long accountId = 101;
        String refreshToken = "refresh_token";

        AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(refreshToken)
                .build();

        AccountTokenDO accountToken = AccountTokenDO.builder()
                .token(refreshToken)
                .associatedAccountId(accountId)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .build();

        // mock
        Mockito.when(accountTokensRepository.getByToken(authRequest.getToken()))
                .thenReturn(Uni.createFrom().item(Optional.of(accountToken)));

        Mockito.when(accountsService.getByIdUnchecked(accountId))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        // do
        assertThatThrownBy(() -> refreshToAccessToken.exchange(authRequest).join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void exchangeInvalidToken() {
        // data
        String refreshToken = "refresh_token";

        AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(refreshToken)
                .build();

        // mock
        Mockito.when(accountTokensRepository.getByToken(authRequest.getToken()))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        // do
        assertThatThrownBy(() -> refreshToAccessToken.exchange(authRequest).join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void exchangeWithTokenOptionChecks() throws InterruptedException {
        // data
        long accountId = 101;
        String refreshToken = "refresh_token";

        AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(refreshToken)
                .clientId("client-1")
                .deviceId("device-1")
                .sourceIp("127.0.0.1")
                .externalSessionId("session-1")
                .userAgent("test")
                .build();

        AccountTokenDO accountToken = AccountTokenDO.builder()
                .token(refreshToken)
                .associatedAccountId(accountId)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .clientId("client-1")
                .deviceId("device-1")
                .sourceIp("127.0.0.1")
                .externalSessionId("session-1")
                .userAgent("test")
                .sourceAuthType("basic")
                .build();

        AccountBO account = AccountBO.builder()
                .id(accountId)
                .build();

        AuthResponseBO newTokens = AuthResponseBO.builder()
                .token("new_token")
                .refreshToken("new_refresh_token")
                .build();

        TokenOptionsBO tokenOptions = TokenOptionsBO.builder()
                .source("basic")
                .userAgent(authRequest.getUserAgent())
                .sourceIp(authRequest.getSourceIp())
                .clientId(authRequest.getClientId())
                .externalSessionId(authRequest.getExternalSessionId())
                .deviceId(authRequest.getDeviceId())
                .build();

        // mock
        Mockito.when(accountTokensRepository.getByToken(authRequest.getToken()))
                .thenReturn(Uni.createFrom().item(Optional.of(accountToken)));

        Mockito.when(accountsService.getByIdUnchecked(accountId))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));

        Mockito.when(accessTokenProvider.generateToken(account, null, tokenOptions))
                .thenReturn(CompletableFuture.completedFuture(newTokens));

        // do
        final AuthResponseBO actual = refreshToAccessToken.exchange(authRequest).join();

        // assert
        assertThat(actual).isEqualTo(newTokens);

        Thread.sleep(50L); // triggering deleting happens in the background so give it some time
        Mockito.verify(accountTokensRepository).deleteToken(refreshToken);
    }

    @Test
    void exchangeWithMismatchedTokenOptions() {
        // data
        long accountId = 101;
        String refreshToken = "refresh_token";

        AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(refreshToken)
                .build();

        AccountTokenDO accountToken = AccountTokenDO.builder()
                .token(refreshToken)
                .associatedAccountId(accountId)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .clientId("client-1")
                .deviceId("device-1")
                .sourceIp("127.0.0.1")
                .externalSessionId("session-1")
                .userAgent("test")
                .build();

        // mock
        Mockito.when(accountTokensRepository.getByToken(authRequest.getToken()))
                .thenReturn(Uni.createFrom().item(Optional.of(accountToken)));

        // do
        assertThatThrownBy(() -> refreshToAccessToken.exchange(authRequest).join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class);

        Mockito.verify(accountTokensRepository, Mockito.never()).deleteToken(refreshToken);
    }
}