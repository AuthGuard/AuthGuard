package com.nexblocks.authguard.jwt;

import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class BasicJtiProviderTest {
    private AccountTokensRepository repository;
    private BasicJtiProvider provider;

    @BeforeEach
    void setup() {
        repository = Mockito.mock(AccountTokensRepository.class);
        provider = new BasicJtiProvider(repository);
    }

    @Test
    void generate() {
        Mockito.when(repository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, AccountTokenDO.class)));

        assertThat(provider.next()).isNotNull();
    }

    @Test
    void notGenerated() {
        Mockito.when(repository.getByToken(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThat(provider.validate("malicious")).isFalse();
    }
}
