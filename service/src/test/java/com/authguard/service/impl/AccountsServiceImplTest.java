package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.dal.AccountsRepository;
import com.authguard.dal.model.EmailDO;
import com.authguard.emb.MessageBus;
import com.authguard.service.PermissionsService;
import com.authguard.service.RolesService;
import com.authguard.service.VerificationMessageService;
import com.authguard.service.config.AccountConfig;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.dal.model.AccountDO;
import com.authguard.service.mappers.ServiceMapperImpl;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AccountEmailBO;
import com.authguard.service.model.PermissionBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountsServiceImplTest {
    private AccountsRepository accountsRepository;
    private PermissionsService permissionsService;
    private RolesService rolesService;
    private VerificationMessageService verificationMessageService;
    private MessageBus messageBus;
    private AccountsServiceImpl accountService;

    private final static EasyRandom RANDOM = new EasyRandom(
            new EasyRandomParameters().collectionSizeRange(3, 5)
    );

    @BeforeAll
    void setup() {
        accountsRepository = Mockito.mock(AccountsRepository.class);
        permissionsService = Mockito.mock(PermissionsService.class);
        rolesService = Mockito.mock(RolesService.class);
        verificationMessageService = Mockito.mock(VerificationMessageService.class);
        messageBus = Mockito.mock(MessageBus.class);

        final ConfigContext configContext = Mockito.mock(ConfigContext.class);

        final AccountConfig accountConfig = AccountConfig.builder()
                .verifyEmail(true)
                .build();

        Mockito.when(configContext.asConfigBean(AccountConfig.class))
                .thenReturn(accountConfig);

        accountService = new AccountsServiceImpl(accountsRepository, permissionsService,
                verificationMessageService, rolesService, new ServiceMapperImpl(), messageBus, configContext);
    }

    @AfterEach
    void resetMocks() {
        Mockito.reset(accountsRepository);
        Mockito.reset(permissionsService);
        Mockito.reset(messageBus);
    }

    @Test
    void create() {
        final AccountBO account = RANDOM.nextObject(AccountBO.class)
                .withDeleted(false)
                .withId(null);

        Mockito.when(accountsRepository.save(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0, AccountDO.class)));

        final AccountBO persisted = accountService.create(account);

        assertThat(persisted).isNotNull();
        assertThat(persisted).isEqualToIgnoringGivenFields(account, "id");

        // need better assertion
        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("accounts"), any());
    }

    @Test
    void getById() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);
        account.setDeleted(false);

        Mockito.when(accountsRepository.getById(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(account)));

        final Optional<AccountBO> retrieved = accountService.getById("");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualToIgnoringGivenFields(account, "permissions", "emails");
        assertThat(retrieved.get().getPermissions()).containsExactly(account.getPermissions().stream()
                .map(permissionDO -> PermissionBO.builder()
                        .group(permissionDO.getGroup())
                        .name(permissionDO.getName())
                        .build()
                ).toArray(PermissionBO[]::new));
    }

    @Test
    void grantPermissions() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(permissionsService.validate(any()))
                .thenAnswer(invocation -> invocation.getArgument(0, List.class));

        final List<PermissionBO> permissions = Arrays.asList(
                RANDOM.nextObject(PermissionBO.class),
                RANDOM.nextObject(PermissionBO.class)
        );

        final AccountBO updated = accountService.grantPermissions(account.getId(), permissions);

        assertThat(updated).isNotEqualTo(account);
        assertThat(updated.getPermissions()).contains(permissions.toArray(new PermissionBO[0]));
    }

    @Test
    void grantPermissionsInvalidPermission() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));

        final List<PermissionBO> permissions = Arrays.asList(
                RANDOM.nextObject(PermissionBO.class),
                RANDOM.nextObject(PermissionBO.class)
        );

        assertThatThrownBy(() -> accountService.grantPermissions(account.getId(), permissions))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void revokePermissions() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);
        final List<PermissionBO> currentPermissions = account.getPermissions().stream()
                .map(permissionDO -> PermissionBO.builder()
                        .group(permissionDO.getGroup())
                        .name(permissionDO.getName())
                        .build()
                ).collect(Collectors.toList());

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));

        final List<PermissionBO> permissionsToRevoke = Arrays.asList(
                currentPermissions.get(0),
                currentPermissions.get(1)
        );

        final AccountBO updated = accountService.revokePermissions(account.getId(), permissionsToRevoke);

        assertThat(updated).isNotEqualTo(account);
        assertThat(updated.getPermissions()).doesNotContain(permissionsToRevoke.toArray(new PermissionBO[0]));
    }

    @Test
    void grantRoles() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final List<String> roles = Arrays.asList(
                RANDOM.nextObject(String.class),
                RANDOM.nextObject(String.class)
        );

        final AccountBO updated = accountService.grantRoles(account.getId(), roles);

        assertThat(updated).isNotEqualTo(account);
        assertThat(updated.getRoles()).contains(roles.toArray(new String[0]));
    }

    @Test
    void revokeRoles() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);
        final List<String> currentRoles = account.getRoles();

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final List<String> rolesToRevoke = Arrays.asList(
                currentRoles.get(0),
                currentRoles.get(1)
        );

        final AccountBO updated = accountService.revokeRoles(account.getId(), rolesToRevoke);

        assertThat(updated).isNotEqualTo(account);
        assertThat(updated.getRoles()).doesNotContain(rolesToRevoke.toArray(new String[0]));
    }

    @Test
    void addEmails() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final List<AccountEmailBO> emails = Arrays.asList(
                RANDOM.nextObject(AccountEmailBO.class),
                RANDOM.nextObject(AccountEmailBO.class)
        );

        final Optional<AccountBO> updated = accountService.addEmails(account.getId(), emails);

        assertThat(updated).isPresent();
        assertThat(updated.get()).isNotEqualTo(account);
        assertThat(updated.get().getEmails()).contains(emails.toArray(new AccountEmailBO[0]));

        // need better assertion
        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("accounts"), any());
    }

    @Test
    void removeEmails() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);
        final List<String> currentEmails = account.getEmails().stream()
                .map(EmailDO::getEmail)
                .collect(Collectors.toList());

        Mockito.when(accountsRepository.getById(account.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
        Mockito.when(accountsRepository.update(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(Optional.of(invocation.getArgument(0, AccountDO.class))));

        final List<String> emailsToRemove = Arrays.asList(
                currentEmails.get(0),
                currentEmails.get(1)
        );

        final Optional<AccountBO> updated = accountService.removeEmails(account.getId(), emailsToRemove);

        assertThat(updated).isPresent();
        assertThat(updated.get()).isNotEqualTo(account);
        assertThat(updated.get().getEmails().stream().map(AccountEmailBO::getEmail).collect(Collectors.toList()))
                .doesNotContain(emailsToRemove.toArray(new String[0]));

        // need better assertion
        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("accounts"), any());
    }
}