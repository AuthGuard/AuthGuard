package org.auther.service.impl;

import org.auther.dal.AccountsRepository;
import org.auther.dal.model.AccountDO;
import org.auther.service.PermissionsService;
import org.auther.service.RolesService;
import org.auther.service.exceptions.ServiceException;
import org.auther.service.model.AccountBO;
import org.auther.service.model.PermissionBO;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountsServiceImplTest {
    private AccountsRepository accountsRepository;
    private PermissionsService permissionsService;
    private RolesService rolesService;
    private AccountsServiceImpl accountService;

    private final static EasyRandom RANDOM = new EasyRandom();

    @BeforeAll
    void setup() {
        accountsRepository = Mockito.mock(AccountsRepository.class);
        permissionsService = Mockito.mock(PermissionsService.class);
        rolesService = Mockito.mock(RolesService.class);
        accountService = new AccountsServiceImpl(accountsRepository, permissionsService, rolesService, new ServiceMapperImpl());
    }

    @AfterEach
    void resetMocks() {
        Mockito.reset(accountsRepository);
        Mockito.reset(permissionsService);
    }

    @Test
    void create() {
        final AccountBO account = RANDOM.nextObject(AccountBO.class)
                .withId(null);

        Mockito.when(accountsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0, AccountDO.class));

        final AccountBO persisted = accountService.create(account);

        assertThat(persisted).isNotNull();
        assertThat(persisted).isEqualToIgnoringGivenFields(account, "id");
    }

    @Test
    void getById() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);

        Mockito.when(accountsRepository.getById(any())).thenReturn(Optional.of(account));

        final Optional<AccountBO> retrieved = accountService.getById("");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualToIgnoringGivenFields(account, "permissions");
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

        Mockito.when(accountsRepository.getById(account.getId())).thenReturn(Optional.of(account));
        Mockito.when(permissionsService.verifyPermissions(any())).thenAnswer(invocation -> invocation.getArgument(0, List.class));

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

        Mockito.when(accountsRepository.getById(account.getId())).thenReturn(Optional.of(account));

        final List<PermissionBO> permissions = Arrays.asList(
                RANDOM.nextObject(PermissionBO.class),
                RANDOM.nextObject(PermissionBO.class)
        );

        assertThatThrownBy(() -> accountService.grantPermissions(account.getId(), permissions)).isInstanceOf(ServiceException.class);
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

        Mockito.when(accountsRepository.getById(account.getId())).thenReturn(Optional.of(account));

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

        Mockito.when(accountsRepository.getById(account.getId())).thenReturn(Optional.of(account));
        Mockito.when(accountsRepository.update(any())).thenAnswer(invocation -> Optional.of(invocation.getArgument(0, AccountDO.class)));

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

        Mockito.when(accountsRepository.getById(account.getId())).thenReturn(Optional.of(account));
        Mockito.when(accountsRepository.update(any())).thenAnswer(invocation -> Optional.of(invocation.getArgument(0, AccountDO.class)));

        final List<String> rolesToRevoke = Arrays.asList(
                currentRoles.get(0),
                currentRoles.get(1)
        );

        final AccountBO updated = accountService.revokeRoles(account.getId(), rolesToRevoke);

        assertThat(updated).isNotEqualTo(account);
        assertThat(updated.getRoles()).doesNotContain(rolesToRevoke.toArray(new String[0]));
    }
}