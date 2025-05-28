package com.nexblocks.authguard.jwt.oauth;

import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorizationCodeVerifierTest {

    @Test
    void verifyAccountToken() {
        AccountTokensRepository accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        AuthorizationCodeVerifier authorizationCodeVerifier = new AuthorizationCodeVerifier(accountTokensRepository);

        long accountId = 101;
        String authorizationCode = "authorization-code";

        AccountTokenDO accountToken = AccountTokenDO.builder()
                .expiresAt(Instant.now().plus(Duration.ofMinutes(5)))
                .associatedAccountId(accountId)
                .token(authorizationCode)
                .build();

        Mockito.when(accountTokensRepository.getByToken(authorizationCode))
                .thenReturn(Uni.createFrom().item(Optional.of(accountToken)));

        assertThat(authorizationCodeVerifier.verifyAccountToken(authorizationCode)).isEqualTo(accountId);
    }

    @Test
    void nonExistingToken() {
        AccountTokensRepository accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        AuthorizationCodeVerifier authorizationCodeVerifier = new AuthorizationCodeVerifier(accountTokensRepository);

        String authorizationCode = "authorization-code";

        Mockito.when(accountTokensRepository.getByToken(authorizationCode))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        assertThatThrownBy(() -> authorizationCodeVerifier.verifyAccountToken(authorizationCode))
                .isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void expiredToken() {
        AccountTokensRepository accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        AuthorizationCodeVerifier authorizationCodeVerifier = new AuthorizationCodeVerifier(accountTokensRepository);

        long accountId = 101;
        String authorizationCode = "authorization-code";

        AccountTokenDO accountToken = AccountTokenDO.builder()
                .expiresAt(Instant.now().minus(Duration.ofMinutes(5)))
                .associatedAccountId(accountId)
                .token(authorizationCode)
                .build();

        Mockito.when(accountTokensRepository.getByToken(authorizationCode))
                .thenReturn(Uni.createFrom().item(Optional.of(accountToken)));

        assertThatThrownBy(() -> authorizationCodeVerifier.verifyAccountToken(authorizationCode))
                .isInstanceOf(ServiceAuthorizationException.class);
    }
}