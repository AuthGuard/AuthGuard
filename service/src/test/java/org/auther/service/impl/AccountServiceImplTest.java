package org.auther.service.impl;

import org.apache.commons.lang3.RandomStringUtils;
import org.auther.dal.AccountsRepository;
import org.auther.dal.model.AccountDO;
import org.auther.service.JWTProvider;
import org.auther.service.SecurePassword;
import org.auther.service.exceptions.ServiceAuthorizationException;
import org.auther.service.exceptions.ServiceException;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountServiceImplTest {
    private AccountsRepository accountsRepository;
    private SecurePassword securePassword;
    private JWTProvider jwtProvider;
    private AccountServiceImpl accountService;

    private final static EasyRandom RANDOM = new EasyRandom();

    @BeforeAll
    void setup() {
        accountsRepository = Mockito.mock(AccountsRepository.class);
        securePassword = Mockito.mock(SecurePassword.class);
        jwtProvider = Mockito.mock(JWTProvider.class);
        accountService = new AccountServiceImpl(accountsRepository, securePassword, jwtProvider);
    }

    @Test
    void create() {
        final AccountBO account = RANDOM.nextObject(AccountBO.class)
                .withId(null);

        Mockito.when(securePassword.hash(any())).thenReturn(RandomStringUtils.randomAlphanumeric(15));
        Mockito.when(accountsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0, AccountDO.class));

        final AccountBO persisted = accountService.create(account);

        assertThat(persisted).isNotNull();
        assertThat(persisted).isEqualToIgnoringGivenFields(account, "id", "password");
        assertThat(persisted.getPassword()).isNullOrEmpty();
    }

    @Test
    void getById() {
        final AccountDO account = RANDOM.nextObject(AccountDO.class);

        Mockito.when(accountsRepository.getById(any())).thenReturn(Optional.of(account));

        final Optional<AccountBO> retrieved = accountService.getById("");

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get()).isEqualToIgnoringGivenFields(account, "password");
    }

    @Test
    void authenticate() {
        final String username = "username";
        final String password = "password";
        final String authorization = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        final AccountDO account = RANDOM.nextObject(AccountDO.class).withUsername(username);
        final TokensBO tokens = RANDOM.nextObject(TokensBO.class);

        Mockito.when(accountsRepository.findByUsername(username)).thenReturn(Optional.of(account));
        Mockito.when(securePassword.verify(password, account.getPassword())).thenReturn(true);
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

        Mockito.when(accountsRepository.findByUsername(username)).thenReturn(Optional.of(account));
        Mockito.when(securePassword.verify(password, account.getPassword())).thenReturn(false);

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
}