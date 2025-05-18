package com.nexblocks.authguard.jwt.oauth;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.config.JacksonConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.jwt.exchange.ImmutablePkceParameters;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationCodeProviderTest {

    private ConfigContext config() {
        ObjectNode configNode = new ObjectNode(JsonNodeFactory.instance)
                .put("lifeTime", "5m")
                .put("randomSize", 128);

        return new JacksonConfigContext(configNode);
    }

    @Test
    void generateToken() {
        AccountTokensRepository accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        AuthorizationCodeProvider authorizationCodeProvider =
                new AuthorizationCodeProvider(accountTokensRepository, new ServiceMapperImpl(), config());

        Mockito.when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, AccountTokenDO.class)));

        AccountBO account = AccountBO.builder()
                .id(101)
                .build();

        AuthResponseBO tokens = authorizationCodeProvider.generateToken(account).join();

        assertThat(tokens.getType()).isEqualTo("authorizationCode");
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNull();

        ArgumentCaptor<AccountTokenDO> argCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository, Mockito.times(1))
                .save(argCaptor.capture());

        assertThat(argCaptor.getValue().getToken()).isEqualTo(tokens.getToken());
        assertThat(argCaptor.getValue().getAssociatedAccountId()).isEqualTo(account.getId());
        assertThat(argCaptor.getValue().getExpiresAt())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(Duration.ofMinutes(6)));
    }

    @Test
    void generateTokenWithOption() {
        AccountTokensRepository accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        AuthorizationCodeProvider authorizationCodeProvider =
                new AuthorizationCodeProvider(accountTokensRepository, new ServiceMapperImpl(), config());

        AccountBO account = AccountBO.builder()
                .id(101)
                .build();

        TokenOptionsBO options = TokenOptionsBO.builder()
                .clientId("client-1")
                .source("basic")
                .deviceId("device-1")
                .sourceIp("127.0.0.1")
                .externalSessionId("session-1")
                .userAgent("test")
                .build();

        Mockito.when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, AccountTokenDO.class)));

        AuthResponseBO tokens = authorizationCodeProvider.generateToken(account, options).join();

        assertThat(tokens.getType()).isEqualTo("authorizationCode");
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNull();

        ArgumentCaptor<AccountTokenDO> argCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);
        AccountTokenDO expected = AccountTokenDO.builder()
                .sourceAuthType("basic")
                .clientId("client-1")
                .deviceId("device-1")
                .sourceIp("127.0.0.1")
                .externalSessionId("session-1")
                .userAgent("test")
                .build();

        Mockito.verify(accountTokensRepository, Mockito.times(1))
                .save(argCaptor.capture());

        assertThat(argCaptor.getValue().getToken()).isEqualTo(tokens.getToken());
        assertThat(argCaptor.getValue().getAssociatedAccountId()).isEqualTo(account.getId());
        assertThat(argCaptor.getValue().getExpiresAt())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(Duration.ofMinutes(6)));

        assertThat(argCaptor.getValue().getSourceAuthType()).isEqualTo(expected.getSourceAuthType());
        assertThat(argCaptor.getValue().getClientId()).isEqualTo(expected.getClientId());
        assertThat(argCaptor.getValue().getDeviceId()).isEqualTo(expected.getDeviceId());
        assertThat(argCaptor.getValue().getSourceIp()).isEqualTo(expected.getSourceIp());
        assertThat(argCaptor.getValue().getExternalSessionId()).isEqualTo(expected.getExternalSessionId());
        assertThat(argCaptor.getValue().getUserAgent()).isEqualTo(expected.getUserAgent());
    }

    @Test
    void generateTokenWithPkce() {
        AccountTokensRepository accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        AuthorizationCodeProvider authorizationCodeProvider =
                new AuthorizationCodeProvider(accountTokensRepository, new ServiceMapperImpl(), config());

        AccountBO account = AccountBO.builder()
                .id(101)
                .build();

        TokenOptionsBO options = TokenOptionsBO.builder()
                .extraParameters(ImmutablePkceParameters.builder()
                        .codeChallenge("random-code-challenge")
                        .codeChallengeMethod("S256")
                        .build())
                .build();

        Mockito.when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, AccountTokenDO.class)));

        AuthResponseBO tokens = authorizationCodeProvider.generateToken(account, options).join();

        assertThat(tokens.getType()).isEqualTo("authorizationCode");
        assertThat(tokens.getToken()).isNotNull();
        assertThat(tokens.getRefreshToken()).isNull();

        ArgumentCaptor<AccountTokenDO> argCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository, Mockito.times(1))
                .save(argCaptor.capture());

        assertThat(argCaptor.getValue().getToken()).isEqualTo(tokens.getToken());
        assertThat(argCaptor.getValue().getAssociatedAccountId()).isEqualTo(account.getId());
        assertThat(argCaptor.getValue().getExpiresAt())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(Duration.ofMinutes(6)));

        assertThat(argCaptor.getValue().getAdditionalInformation().get("codeChallenge"))
                .isEqualTo("random-code-challenge");
        assertThat(argCaptor.getValue().getAdditionalInformation().get("codeChallengeMethod"))
                .isEqualTo("S256");
    }
}