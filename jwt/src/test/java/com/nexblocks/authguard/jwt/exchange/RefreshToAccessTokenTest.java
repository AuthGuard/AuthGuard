package com.nexblocks.authguard.jwt.exchange;

import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.dal.model.TokenRestrictionsDO;
import com.nexblocks.authguard.jwt.AccessTokenProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenRestrictionsBO;
import io.vavr.control.Either;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

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
                accessTokenProvider, new ServiceMapperImpl());
    }

    @Test
    void exchange() {
        // data
        final String accountId = "account";
        final String refreshToken = "refresh_token";

        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(refreshToken)
                .build();

        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .token(refreshToken)
                .associatedAccountId(accountId)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .build();

        final AccountBO account = AccountBO.builder()
                .id(accountId)
                .build();

        final AuthResponseBO newTokens = AuthResponseBO.builder()
                .token("new_token")
                .refreshToken("new_refresh_token")
                .build();

        // mock
        Mockito.when(accountTokensRepository.getByToken(authRequest.getToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountToken)));

        Mockito.when(accountsService.getById(accountId))
                .thenReturn(Optional.of(account));

        Mockito.when(accessTokenProvider.generateToken(account, (TokenRestrictionsBO) null))
                .thenReturn(newTokens);

        // do
        final Either<Exception, AuthResponseBO> actual = refreshToAccessToken.exchange(authRequest);

        // assert
        assertThat(actual.isRight()).isTrue();
        assertThat(actual.right().get()).isEqualTo(newTokens);

        Mockito.verify(accountTokensRepository).deleteToken(refreshToken);
    }

    @Test
    void exchangeWithRestrictions() {
        // data
        final String accountId = "account";
        final String refreshToken = "refresh_token";
        final String restrictionPermission = "permission.read";

        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(refreshToken)
                .build();

        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .token(refreshToken)
                .associatedAccountId(accountId)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .tokenRestrictions(TokenRestrictionsDO.builder()
                        .permissions(Collections.singleton(restrictionPermission))
                        .scopes(Collections.emptySet())
                        .build())
                .build();

        final AccountBO account = AccountBO.builder()
                .id(accountId)
                .build();

        final AuthResponseBO newTokens = AuthResponseBO.builder()
                .token("new_token")
                .refreshToken("new_refresh_token")
                .build();

        // mock
        Mockito.when(accountTokensRepository.getByToken(authRequest.getToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountToken)));

        Mockito.when(accountsService.getById(accountId))
                .thenReturn(Optional.of(account));

        Mockito.when(accessTokenProvider.generateToken(account, TokenRestrictionsBO.builder()
                .addPermissions(restrictionPermission)
                .build())).thenReturn(newTokens);

        // do
        final Either<Exception, AuthResponseBO> actual = refreshToAccessToken.exchange(authRequest);

        // assert
        assertThat(actual.isRight()).isTrue();
        assertThat(actual.right().get()).isEqualTo(newTokens);

        Mockito.verify(accountTokensRepository).deleteToken(refreshToken);
    }

    @Test
    void exchangeExpiredToken() {
        // data
        final String accountId = "account";
        final String refreshToken = "refresh_token";

        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(refreshToken)
                .build();

        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .token(refreshToken)
                .associatedAccountId(accountId)
                .expiresAt(Instant.now().minus(Duration.ofMinutes(1)))
                .build();

        // mock
        Mockito.when(accountTokensRepository.getByToken(authRequest.getToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountToken)));

        // do
        final Either<Exception, AuthResponseBO> actual = refreshToAccessToken.exchange(authRequest);

        // assert
        assertThat(actual.isLeft()).isTrue();
        assertThat(actual.left().get()).isInstanceOf(ServiceAuthorizationException.class);

        Mockito.verify(accountTokensRepository).deleteToken(refreshToken);
    }

    @Test
    void exchangeNoAccount() {
        // data
        final String accountId = "account";
        final String refreshToken = "refresh_token";

        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(refreshToken)
                .build();

        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .token(refreshToken)
                .associatedAccountId(accountId)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .build();

        // mock
        Mockito.when(accountTokensRepository.getByToken(authRequest.getToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountToken)));

        Mockito.when(accountsService.getById(accountId))
                .thenReturn(Optional.empty());

        // do
        final Either<Exception, AuthResponseBO> actual = refreshToAccessToken.exchange(authRequest);

        // assert
        assertThat(actual.isLeft()).isTrue();
        assertThat(actual.left().get()).isInstanceOf(ServiceAuthorizationException.class);

        Mockito.verify(accountTokensRepository).deleteToken(refreshToken);
    }

    @Test
    void exchangeInvalidToken() {
        // data
        final String refreshToken = "refresh_token";

        final AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(refreshToken)
                .build();

        // mock
        Mockito.when(accountTokensRepository.getByToken(authRequest.getToken()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        // do
        final Either<Exception, AuthResponseBO> actual = refreshToAccessToken.exchange(authRequest);

        // assert
        assertThat(actual.isLeft()).isTrue();
        assertThat(actual.left().get()).isInstanceOf(ServiceAuthorizationException.class);
    }
}