package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.dal.persistence.CredentialsAuditRepository;
import com.nexblocks.authguard.dal.persistence.CredentialsRepository;
import com.nexblocks.authguard.dal.model.CredentialsAuditDO;
import com.nexblocks.authguard.dal.model.CredentialsDO;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.IdempotencyService;
import com.nexblocks.authguard.service.config.PasswordConditions;
import com.nexblocks.authguard.service.config.PasswordsConfig;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.basic.passwords.ServiceInvalidPasswordException;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.CredentialsBO;
import com.nexblocks.authguard.service.model.HashedPasswordBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.basic.passwords.PasswordValidator;
import com.nexblocks.authguard.basic.passwords.SecurePassword;
import org.assertj.core.api.Assertions;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CredentialsServiceImplTest {
    private AccountsService accountsService;
    private IdempotencyService idempotencyService;
    private CredentialsRepository credentialsRepository;
    private CredentialsAuditRepository credentialsAuditRepository;
    private SecurePassword securePassword;
    private CredentialsServiceImpl credentialsService;
    private MessageBus messageBus;

    private final static EasyRandom RANDOM = new EasyRandom(new EasyRandomParameters().collectionSizeRange(2, 4));

    @BeforeAll
    void setup() {
        accountsService = Mockito.mock(AccountsService.class);
        idempotencyService = Mockito.mock(IdempotencyService.class);
        credentialsRepository = Mockito.mock(CredentialsRepository.class);
        credentialsAuditRepository = Mockito.mock(CredentialsAuditRepository.class);
        securePassword = Mockito.mock(SecurePassword.class);
        messageBus = Mockito.mock(MessageBus.class);

        final PasswordValidator passwordValidator = new PasswordValidator(PasswordsConfig.builder()
                .conditions(PasswordConditions.builder().build()).build());

        credentialsService = new CredentialsServiceImpl(accountsService, idempotencyService, credentialsRepository,
                credentialsAuditRepository, securePassword, passwordValidator, messageBus, new ServiceMapperImpl());
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
        final CredentialsBO credentials = RANDOM.nextObject(CredentialsBO.class)
                .withPlainPassword("SecurePassword77$3");
        final HashedPasswordBO hashedPassword = RANDOM.nextObject(HashedPasswordBO.class);

        final String idempotentKey = "idempotent-key";
        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .build();

        Mockito.when(securePassword.hash(any())).thenReturn(hashedPassword);
        Mockito.when(credentialsRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, CredentialsDO.class)));

        Mockito.when(idempotencyService.performOperation(Mockito.any(), Mockito.eq(idempotentKey), Mockito.eq(credentials.getEntityType())))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, Supplier.class).get()));

        final CredentialsBO persisted = credentialsService.create(credentials, requestContext);

        assertThat(persisted).isNotNull();
        assertThat(persisted).isEqualToIgnoringGivenFields(credentials, "id", "plainPassword", "hashedPassword");
        assertThat(persisted.getHashedPassword()).isNull();

        // need better assertion
        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("credentials"), any());
    }

    @Test
    void createNonExistingAccount() {
        final CredentialsBO credentials = RANDOM.nextObject(CredentialsBO.class);
        final String idempotentKey = "idempotent-key";
        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .build();

        Mockito.when(accountsService.getById(credentials.getAccountId())).thenReturn(Optional.empty());
        Mockito.when(idempotencyService.performOperation(Mockito.any(), Mockito.eq(idempotentKey), Mockito.eq(credentials.getEntityType())))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, Supplier.class).get()));

        assertThatThrownBy(() -> credentialsService.create(credentials, requestContext))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void createWithInvalidPassword() {
        final CredentialsBO credentials = RANDOM.nextObject(CredentialsBO.class)
                .withPlainPassword("bad");
        final String idempotentKey = "idempotent-key";
        final RequestContextBO requestContext = RequestContextBO.builder()
                .idempotentKey(idempotentKey)
                .build();

        Mockito.when(idempotencyService.performOperation(Mockito.any(), Mockito.eq(idempotentKey), Mockito.eq(credentials.getEntityType())))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, Supplier.class).get()));

        assertThatThrownBy(() -> credentialsService.create(credentials, requestContext))
                .isInstanceOf(ServiceInvalidPasswordException.class);
    }

    @Test
    void getById() {
        final CredentialsDO credentials = RANDOM.nextObject(CredentialsDO.class);

        Mockito.when(credentialsRepository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));

        final Optional<CredentialsBO> retrieved = credentialsService.getById("");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualToIgnoringGivenFields(credentials,
                "hashedPassword", "plainPassword", "identifiers", "entityType");
        assertThat(retrieved.get().getHashedPassword()).isNull();
        assertThat(retrieved.get().getPlainPassword()).isNull();
    }

    @Test
    void getByIdNotFound() {
        Mockito.when(credentialsRepository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        Assertions.assertThat(credentialsService.getById("")).isEmpty();
    }

    @Test
    void getByUsername() {
        final CredentialsDO credentials = RANDOM.nextObject(CredentialsDO.class);

        Mockito.when(credentialsRepository.findByIdentifier(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(credentials)));

        final Optional<CredentialsBO> retrieved = credentialsService.getByUsername("");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualToIgnoringGivenFields(credentials,
                "hashedPassword", "plainPassword", "identifiers", "entityType");
        assertThat(retrieved.get().getHashedPassword()).isNull();
        assertThat(retrieved.get().getPlainPassword()).isNull();
    }

    @Test
    void getByUsernameNotFound() {
        Mockito.when(credentialsRepository.findByIdentifier(any()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        Assertions.assertThat(credentialsService.getByUsername("")).isEmpty();
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
}