package org.auther.service.impl;

import org.apache.commons.lang3.RandomStringUtils;
import org.auther.dal.AccountsRepository;
import org.auther.dal.model.AccountDO;
import org.auther.service.JWTProvider;
import org.auther.service.SecurePassword;
import org.auther.service.exceptions.ServiceAuthorizationException;
import org.auther.service.exceptions.ServiceException;
import org.auther.service.model.AccountBO;
import org.auther.service.model.HashedPasswordBO;
import org.auther.service.model.PermissionBO;
import org.auther.service.model.TokensBO;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountsServiceImplTest {
    private AccountsRepository accountsRepository;
    private SecurePassword securePassword;
    private JWTProvider jwtProvider;
    private AccountsServiceImpl accountService;

    private final static EasyRandom RANDOM = new EasyRandom();

    @BeforeAll
    void setup() {
        accountsRepository = Mockito.mock(AccountsRepository.class);
        securePassword = Mockito.mock(SecurePassword.class);
        jwtProvider = Mockito.mock(JWTProvider.class);
        accountService = new AccountsServiceImpl(accountsRepository, securePassword, jwtProvider);
    }

    @Test
    void create() {
        final AccountBO account = RANDOM.nextObject(AccountBO.class)
                .withId(null);
        final HashedPasswordBO hashedPassword = RANDOM.nextObject(HashedPasswordBO.class);

        Mockito.when(securePassword.hash(any())).thenReturn(hashedPassword);
        Mockito.when(accountsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0, AccountDO.class));

        final AccountBO persisted = accountService.create(account);

        assertThat(persisted).isNotNull();
        assertThat(persisted).isEqualToIgnoringGivenFields(account, "id", "plainPassword", "hashedPassword");
        assertThat(persisted.getHashedPassword()).isNull();
    }

    @Test
    void getById() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);

        Mockito.when(accountsRepository.getById(any())).thenReturn(Optional.of(account));

        final Optional<AccountBO> retrieved = accountService.getById("");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualToIgnoringGivenFields(account, "hashedPassword", "plainPassword", "permissions");
        assertThat(retrieved.get().getHashedPassword()).isNull();
        assertThat(retrieved.get().getPlainPassword()).isNull();
        assertThat(retrieved.get().getPermissions()).containsExactly(account.getPermissions().stream()
                .map(permissionDO -> PermissionBO.builder()
                        .group(permissionDO.getGroup())
                        .name(permissionDO.getName())
                        .build()
                ).toArray(PermissionBO[]::new));
    }

    @Test
    void authenticate() {
        final String username = "username";
        final String password = "password";
        final String authorization = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        final AccountDO account = RANDOM.nextObject(AccountDO.class).withUsername(username);
        final TokensBO tokens = RANDOM.nextObject(TokensBO.class);
        final HashedPasswordBO hashedPasswordBO = HashedPasswordBO.builder()
                .password(account.getHashedPassword().getPassword())
                .salt(account.getHashedPassword().getSalt())
                .build();

        Mockito.when(accountsRepository.findByUsername(username)).thenReturn(Optional.of(account));
        Mockito.when(securePassword.verify(eq(password), eq(hashedPasswordBO))).thenReturn(true);
        Mockito.when(jwtProvider.generateToken(any())).thenReturn(tokens);

        final Optional<TokensBO> retrieved = accountService.authenticate(authorization);

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getToken()).isNotNull();
        assertThat(retrieved.get().getRefreshToken()).isNotNull();
    }

    @Test
    void authenticateNotFound() {
        final String username = "username";
        final String password = "password";
        final String authorization = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        Mockito.when(accountsRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThat(accountService.authenticate(authorization)).isEmpty();
    }

    @Test
    void authenticateWrongPassword() {
        final String username = "username";
        final String password = "password";
        final String authorization = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        final AccountDO account = RANDOM.nextObject(AccountDO.class).withUsername(username);
        final HashedPasswordBO hashedPasswordBO = HashedPasswordBO.builder()
                .password(account.getHashedPassword().getPassword())
                .salt(account.getHashedPassword().getSalt())
                .build();

        Mockito.when(accountsRepository.findByUsername(username)).thenReturn(Optional.of(account));
        Mockito.when(securePassword.verify(eq(password), eq(hashedPasswordBO))).thenReturn(false);

        assertThatThrownBy(() -> accountService.authenticate(authorization)).isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void authenticateBadAuthorization() {
        final String authorization = RandomStringUtils.randomAlphanumeric(20);
        assertThatThrownBy(() -> accountService.authenticate(authorization)).isInstanceOf(ServiceException.class);
    }

    @Test
    void authenticateUnsupportedScheme() {
        final String authorization = "Unsupported " + RandomStringUtils.randomAlphanumeric(20);
        assertThatThrownBy(() -> accountService.authenticate(authorization)).isInstanceOf(ServiceException.class);
    }

    @Test
    void authenticateBadBasicScheme() {
        final String authorization = "Basic dGhpc2RvbmVzbid0Zmx5aW5vdXJjaXR5";
        assertThatThrownBy(() -> accountService.authenticate(authorization)).isInstanceOf(ServiceException.class);
    }

    @Test
    void grantPermissions() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);

        Mockito.when(accountsRepository.getById(account.getId())).thenReturn(Optional.of(account));

        final List<PermissionBO> permissions = Arrays.asList(
                RANDOM.nextObject(PermissionBO.class),
                RANDOM.nextObject(PermissionBO.class)
        );

        final AccountBO updated = accountService.grantPermissions(account.getId(), permissions);

        assertThat(updated).isNotEqualTo(account);
        assertThat(updated.getPermissions()).contains(permissions.toArray(new PermissionBO[0]));
        assertThat(updated.getHashedPassword()).isNull();
        assertThat(updated.getPlainPassword()).isNull();
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
        assertThat(updated.getHashedPassword()).isNull();
        assertThat(updated.getPlainPassword()).isNull();
    }
}