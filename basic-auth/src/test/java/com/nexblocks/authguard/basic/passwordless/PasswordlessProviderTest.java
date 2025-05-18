package com.nexblocks.authguard.basic.passwordless;

import com.nexblocks.authguard.basic.config.PasswordlessConfig;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class PasswordlessProviderTest {
    private AccountTokensRepository accountTokensRepository;
    private MessageBus messageBus;

    private PasswordlessProvider passwordlessProvider;

    void setup(final PasswordlessConfig passwordlessConfig) {
        accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        messageBus = Mockito.mock(MessageBus.class);

        ConfigContext configContext = Mockito.mock(ConfigContext.class);

        Mockito.when(configContext.asConfigBean(PasswordlessConfig.class)).thenReturn(passwordlessConfig);
        Mockito.when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, AccountTokenDO.class)));

        passwordlessProvider = new PasswordlessProvider(accountTokensRepository, messageBus, configContext);
    }

    @Test
    void generateToken() {
        PasswordlessConfig passwordlessConfig = PasswordlessConfig.builder()
                .randomSize(32)
                .tokenLife("5m")
                .build();

        setup(passwordlessConfig);

        AccountBO account = AccountBO.builder()
                .id(101)
                .active(true)
                .build();

        TokenOptionsBO tokenOptions = TokenOptionsBO.builder().build();

        AuthResponseBO expected = AuthResponseBO.builder()
                .type("passwordless")
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .build();

        AuthResponseBO generated = passwordlessProvider.generateToken(account, tokenOptions).join();

        assertThat(generated).isEqualToIgnoringGivenFields(expected, "token");
        assertThat(generated.getToken()).isNotNull();

        ArgumentCaptor<AccountTokenDO> argumentCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(argumentCaptor.capture());

        AccountTokenDO persisted = argumentCaptor.getValue();

        assertThat(persisted.getAssociatedAccountId()).isEqualTo(account.getId());
        assertThat(persisted.getExpiresAt())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(Duration.ofMinutes(6)));
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getToken()).isNotNull();

        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("passwordless"), any());
    }

    @Test
    void generateTokenForInactiveAccount() {
        PasswordlessConfig passwordlessConfig = PasswordlessConfig.builder()
                .randomSize(32)
                .tokenLife("5m")
                .build();

        setup(passwordlessConfig);

        AccountBO account = AccountBO.builder()
                .active(false)
                .build();

        TokenOptionsBO tokenOptions = TokenOptionsBO.builder().build();

        assertThatThrownBy(() -> passwordlessProvider.generateToken(account, tokenOptions))
                .isInstanceOf(ServiceAuthorizationException.class);
    }
}