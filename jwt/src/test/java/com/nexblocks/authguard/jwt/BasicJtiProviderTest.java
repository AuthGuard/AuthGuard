package com.nexblocks.authguard.jwt;

import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import io.smallrye.mutiny.Uni;

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
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, AccountTokenDO.class)));

        assertThat(provider.next()).isNotNull();
    }

    @Test
    void notGenerated() {
        Mockito.when(repository.getByToken(any()))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        assertThat(provider.validate("malicious")).isFalse();
    }
}
