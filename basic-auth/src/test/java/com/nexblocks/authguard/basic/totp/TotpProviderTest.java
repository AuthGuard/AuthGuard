package com.nexblocks.authguard.basic.totp;

import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
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

class TotpProviderTest {
    private AccountTokensRepository accountTokensRepository;
    private MessageBus messageBus;

    private TotpProvider totpProvider;

    @BeforeEach
    void setup() {
        accountTokensRepository = Mockito.mock(AccountTokensRepository.class);
        messageBus = Mockito.mock(MessageBus.class);

        Mockito.when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, AccountTokenDO.class)));

        totpProvider = new TotpProvider(accountTokensRepository, new ServiceMapperImpl(),
                messageBus);
    }

    @Test
    void generateToken() {
        AccountBO account = AccountBO.builder()
                .id(1)
                .active(true)
                .domain("test")
                .build();

        TokenOptionsBO tokenOptions = TokenOptionsBO.builder().build();

        AuthResponseBO generated = totpProvider.generateToken(account, tokenOptions).join();

        AuthResponseBO expected = AuthResponseBO.builder()
                .type("totp")
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .build();

        assertThat(generated)
                .usingRecursiveComparison()
                .ignoringFields("token")
                .isEqualTo(expected);
        assertThat(generated.getToken()).isNotNull();

        ArgumentCaptor<AccountTokenDO> argumentCaptor = ArgumentCaptor.forClass(AccountTokenDO.class);

        Mockito.verify(accountTokensRepository).save(argumentCaptor.capture());

        AccountTokenDO persisted = argumentCaptor.getValue();

        assertThat(persisted.getAssociatedAccountId()).isEqualTo(account.getId());
        assertThat(persisted.getExpiresAt())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(Duration.ofMinutes(3)));
        assertThat(persisted.getToken()).isNotNull();

        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("totp"), any());
    }

    @Test
    void generateTokenForInactiveAccount() {
        AccountBO account = AccountBO.builder()
                .id(1)
                .active(false)
                .domain("test")
                .build();
        TokenOptionsBO tokenOptions = TokenOptionsBO.builder().build();

        assertThatThrownBy(() -> totpProvider.generateToken(account, tokenOptions))
                .isInstanceOf(ServiceAuthorizationException.class);
    }
}