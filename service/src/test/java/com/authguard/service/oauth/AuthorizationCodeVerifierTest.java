package com.authguard.service.oauth;

import com.authguard.dal.AccountTokensRepository;
import com.authguard.dal.model.AccountTokenDO;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationCodeVerifierTest {

    @Test
    void verifyAccountToken() {
        final AccountTokensRepository accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        final AuthorizationCodeVerifier authorizationCodeVerifier = new AuthorizationCodeVerifier(accountTokensRepository);

        final String accountId = "account-id";
        final String authorizationCode = "authorization-code";

        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .expiresAt(ZonedDateTime.now().plus(Duration.ofMinutes(5)))
                .associatedAccountId(accountId)
                .token(authorizationCode)
                .build();

        Mockito.when(accountTokensRepository.getByToken(authorizationCode))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountToken)));

        assertThat(authorizationCodeVerifier.verifyAccountToken(authorizationCode)).contains(accountId);
    }

    @Test
    void nonExistingToken() {
        final AccountTokensRepository accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        final AuthorizationCodeVerifier authorizationCodeVerifier = new AuthorizationCodeVerifier(accountTokensRepository);

        final String authorizationCode = "authorization-code";

        Mockito.when(accountTokensRepository.getByToken(authorizationCode))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> authorizationCodeVerifier.verifyAccountToken(authorizationCode))
                .isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void expiredToken() {
        final AccountTokensRepository accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        final AuthorizationCodeVerifier authorizationCodeVerifier = new AuthorizationCodeVerifier(accountTokensRepository);

        final String accountId = "account-id";
        final String authorizationCode = "authorization-code";

        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .expiresAt(ZonedDateTime.now().minus(Duration.ofMinutes(5)))
                .associatedAccountId(accountId)
                .token(authorizationCode)
                .build();

        Mockito.when(accountTokensRepository.getByToken(authorizationCode))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountToken)));

        assertThatThrownBy(() -> authorizationCodeVerifier.verifyAccountToken(authorizationCode))
                .isInstanceOf(ServiceAuthorizationException.class);
    }
}