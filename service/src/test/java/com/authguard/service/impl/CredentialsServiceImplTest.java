package com.authguard.service.impl;

import com.authguard.dal.CredentialsAuditRepository;
import com.authguard.dal.CredentialsRepository;
import com.authguard.emb.MessageBus;
import com.authguard.service.AccountsService;
import com.authguard.service.passwords.SecurePassword;
import com.authguard.dal.model.CredentialsAuditDO;
import com.authguard.dal.model.CredentialsDO;
import com.authguard.service.exceptions.ServiceConflictException;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.mappers.ServiceMapperImpl;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.CredentialsBO;
import com.authguard.service.model.HashedPasswordBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CredentialsServiceImplTest {
    private AccountsService accountsService;
    private CredentialsRepository credentialsRepository;
    private CredentialsAuditRepository credentialsAuditRepository;
    private SecurePassword securePassword;
    private CredentialsServiceImpl credentialsService;
    private MessageBus messageBus;

    private final static EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters().collectionSizeRange(2, 4));

    @BeforeAll
    void setup() {
        accountsService = Mockito.mock(AccountsService.class);
        credentialsRepository = Mockito.mock(CredentialsRepository.class);
        credentialsAuditRepository = Mockito.mock(CredentialsAuditRepository.class);
        securePassword = Mockito.mock(SecurePassword.class);
        messageBus = Mockito.mock(MessageBus.class);

        credentialsService = new CredentialsServiceImpl(accountsService, credentialsRepository,
                credentialsAuditRepository, securePassword, messageBus, new ServiceMapperImpl());
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(accountsService);
        Mockito.reset(credentialsRepository);
        Mockito.reset(credentialsAuditRepository);
        Mockito.reset(messageBus);

        Mockito.when(credentialsRepository.findByIdentifier(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));
        Mockito.when(accountsService.getById(any())).thenReturn(Optional.of(RANDOM.nextObject(AccountBO.class)));
    }

    @Test
    void create() {
        final CredentialsBO credentials = RANDOM.nextObject(CredentialsBO.class);
        final HashedPasswordBO hashedPassword = RANDOM.nextObject(HashedPasswordBO.class);

        Mockito.when(securePassword.hash(any())).thenReturn(hashedPassword);
        Mockito.when(credentialsRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsDO.class)));

        final CredentialsBO persisted = credentialsService.create(credentials);

        assertThat(persisted).isNotNull();
        assertThat(persisted).isEqualToIgnoringGivenFields(credentials, "id", "plainPassword", "hashedPassword");
        assertThat(persisted.getHashedPassword()).isNull();

        // need better assertion
        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("credentials"), any());
    }

    @Test
    void createDuplicateUsername() {
        final CredentialsBO credentials = RANDOM.nextObject(CredentialsBO.class);

        Mockito.when(credentialsRepository.findByIdentifier(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(CredentialsDO.builder().build())));

        assertThatThrownBy(() -> credentialsService.create(credentials)).isInstanceOf(ServiceConflictException.class);
    }

    @Test
    void createNonExistingAccount() {
        final CredentialsBO credentials = RANDOM.nextObject(CredentialsBO.class);

        Mockito.when(accountsService.getById(credentials.getAccountId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> credentialsService.create(credentials)).isInstanceOf(ServiceException.class);
    }

    @Test
    void getById() {
        final CredentialsDO credentials = RANDOM.nextObject(CredentialsDO.class);

        Mockito.when(credentialsRepository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));

        final Optional<CredentialsBO> retrieved = credentialsService.getById("");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualToIgnoringGivenFields(credentials, "hashedPassword", "plainPassword", "identifiers");
        assertThat(retrieved.get().getHashedPassword()).isNull();
        assertThat(retrieved.get().getPlainPassword()).isNull();
    }

    @Test
    void getByIdNotFound() {
        Mockito.when(credentialsRepository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThat(credentialsService.getById("")).isEmpty();
    }

    @Test
    void getByUsername() {
        final CredentialsDO credentials = RANDOM.nextObject(CredentialsDO.class);

        Mockito.when(credentialsRepository.findByIdentifier(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));

        final Optional<CredentialsBO> retrieved = credentialsService.getByUsername("");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualToIgnoringGivenFields(credentials, "hashedPassword", "plainPassword", "identifiers");
        assertThat(retrieved.get().getHashedPassword()).isNull();
        assertThat(retrieved.get().getPlainPassword()).isNull();
    }

    @Test
    void getByUsernameNotFound() {
        Mockito.when(credentialsRepository.findByIdentifier(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThat(credentialsService.getByUsername("")).isEmpty();
    }

    @Test
    void update() {
        final CredentialsDO credentials = RANDOM.nextObject(CredentialsDO.class);
        final CredentialsBO update = RANDOM.nextObject(CredentialsBO.class).withId(credentials.getId());

        Mockito.when(credentialsRepository.getById(credentials.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));
        Mockito.when(credentialsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, CredentialsDO.class))));
        Mockito.when(credentialsAuditRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsAuditDO.class)));

        final Optional<CredentialsBO> result = credentialsService.update(update);

        assertThat(result).isPresent();
        assertThat(result.get().getHashedPassword()).isNull();
        assertThat(result.get().getPlainPassword()).isNull();

        // verify call to audit repository
        final ArgumentCaptor<CredentialsAuditDO> argumentCaptor = ArgumentCaptor.forClass(CredentialsAuditDO.class);
        Mockito.verify(credentialsAuditRepository, Mockito.times(2)).save(argumentCaptor.capture());

        final List<CredentialsAuditDO> auditArgs = argumentCaptor.getAllValues();

        assertThat(auditArgs.size()).isEqualTo(2);

        assertThat(auditArgs.get(0)).isNotNull();
        assertThat(auditArgs.get(0).getCredentialsId()).isEqualTo(credentials.getId());
        assertThat(auditArgs.get(0).getAction()).isEqualTo(CredentialsAuditDO.Action.ATTEMPT);
        assertThat(auditArgs.get(0).getPassword()).isNull();

        assertThat(auditArgs.get(0)).isNotNull();
        assertThat(auditArgs.get(1).getCredentialsId()).isEqualTo(credentials.getId());
        assertThat(auditArgs.get(1).getAction()).isEqualTo(CredentialsAuditDO.Action.UPDATED);
        assertThat(auditArgs.get(1).getPassword()).isNull();

        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("credentials"), any());
    }

    @Test
    void updatePassword() {
        final CredentialsDO credentials = RANDOM.nextObject(CredentialsDO.class);
        final CredentialsBO update = RANDOM.nextObject(CredentialsBO.class).withId(credentials.getId());

        Mockito.when(credentialsRepository.getById(credentials.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));
        Mockito.when(credentialsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, CredentialsDO.class))));
        Mockito.when(credentialsAuditRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsAuditDO.class)));

        final Optional<CredentialsBO> result = credentialsService.updatePassword(update);

        assertThat(result).isPresent();
        assertThat(result.get().getHashedPassword()).isNull();
        assertThat(result.get().getPlainPassword()).isNull();

        // verify call to audit repository
        final ArgumentCaptor<CredentialsAuditDO> argumentCaptor = ArgumentCaptor.forClass(CredentialsAuditDO.class);
        Mockito.verify(credentialsAuditRepository, Mockito.times(2)).save(argumentCaptor.capture());

        final List<CredentialsAuditDO> auditArgs = argumentCaptor.getAllValues();

        assertThat(auditArgs.size()).isEqualTo(2);

        assertThat(auditArgs.get(0)).isNotNull();
        assertThat(auditArgs.get(0).getCredentialsId()).isEqualTo(credentials.getId());
        assertThat(auditArgs.get(0).getAction()).isEqualTo(CredentialsAuditDO.Action.ATTEMPT);
        assertThat(auditArgs.get(0).getPassword()).isNull();

        assertThat(auditArgs.get(0)).isNotNull();
        assertThat(auditArgs.get(1).getCredentialsId()).isEqualTo(credentials.getId());
        assertThat(auditArgs.get(1).getAction()).isEqualTo(CredentialsAuditDO.Action.UPDATED);
        assertThat(auditArgs.get(1).getPassword()).isNotNull();

        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("credentials"), any());
    }

    @Test
    void updateDuplicateUsername() {
        final CredentialsBO credentials = RANDOM.nextObject(CredentialsBO.class);

        Mockito.when(credentialsRepository.findByIdentifier(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(CredentialsDO.builder()
                        .accountId("")
                        .build())));

        assertThatThrownBy(() -> credentialsService.update(credentials)).isInstanceOf(ServiceConflictException.class);
    }
}
