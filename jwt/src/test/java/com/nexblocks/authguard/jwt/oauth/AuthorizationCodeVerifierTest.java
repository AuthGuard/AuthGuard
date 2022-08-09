package com.nexblocks.authguard.jwt.oauth;

import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import io.vavr.control.Either;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
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
                .expiresAt(Instant.now().plus(Duration.ofMinutes(5)))
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

        final Either<Exception, String> result = authorizationCodeVerifier.verifyAccountToken(authorizationCode);

        assertThat(result.isLeft());
        assertThat(result.getLeft()).isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void expiredToken() {
        final AccountTokensRepository accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        final AuthorizationCodeVerifier authorizationCodeVerifier = new AuthorizationCodeVerifier(accountTokensRepository);

        final String accountId = "account-id";
        final String authorizationCode = "authorization-code";

        final AccountTokenDO accountToken = AccountTokenDO.builder()
                .expiresAt(Instant.now().minus(Duration.ofMinutes(5)))
                .associatedAccountId(accountId)
                .token(authorizationCode)
                .build();

        Mockito.when(accountTokensRepository.getByToken(authorizationCode))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(accountToken)));

        assertThatThrownBy(() -> authorizationCodeVerifier.verifyAccountToken(authorizationCode))
                .isInstanceOf(ServiceAuthorizationException.class);
    }
}