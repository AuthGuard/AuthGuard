package org.auther.service.impl;

import org.auther.dal.CredentialsRepository;
import org.auther.dal.model.CredentialsDO;
import org.auther.service.SecurePassword;
import org.auther.service.model.CredentialsBO;
import org.auther.service.model.HashedPasswordBO;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CredentialsServiceImplTest {
    private CredentialsRepository credentialsRepository;
    private SecurePassword securePassword;
    private CredentialsServiceImpl accountService;

    private final static EasyRandom RANDOM = new EasyRandom();

    @BeforeAll
    void setup() {
        credentialsRepository = Mockito.mock(CredentialsRepository.class);
        securePassword = Mockito.mock(SecurePassword.class);
        accountService = new CredentialsServiceImpl(credentialsRepository, securePassword, new ServiceMapperImpl());
    }

    @AfterEach
    void resetMocks() {
        Mockito.reset(credentialsRepository);
    }

    @Test
    void create() {
        final CredentialsBO account = RANDOM.nextObject(CredentialsBO.class);
        final HashedPasswordBO hashedPassword = RANDOM.nextObject(HashedPasswordBO.class);

        Mockito.when(securePassword.hash(any())).thenReturn(hashedPassword);
        Mockito.when(credentialsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0, CredentialsDO.class));

        final CredentialsBO persisted = accountService.create(account);

        assertThat(persisted).isNotNull();
        assertThat(persisted).isEqualToIgnoringGivenFields(account, "id", "plainPassword", "hashedPassword");
        assertThat(persisted.getHashedPassword()).isNull();
    }

    @Test
    void getById() {
        final CredentialsDO account = RANDOM.nextObject(CredentialsDO.class);

        Mockito.when(credentialsRepository.getById(any())).thenReturn(Optional.of(account));

        final Optional<CredentialsBO> retrieved = accountService.getById("");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualToIgnoringGivenFields(account, "hashedPassword", "plainPassword", "permissions");
        assertThat(retrieved.get().getHashedPassword()).isNull();
        assertThat(retrieved.get().getPlainPassword()).isNull();
    }
}
