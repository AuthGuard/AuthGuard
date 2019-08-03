package org.auther.service.impl;

import org.auther.dal.AccountsRepository;
import org.auther.service.JWTProvider;
import org.auther.service.SecurePassword;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountServiceImplTest {
    private AccountsRepository accountsRepository;
    private SecurePassword securePassword;
    private JWTProvider jwtProvider;
    private AccountServiceImpl accountService;

    @BeforeAll
    void setup() {
        accountsRepository = Mockito.mock(AccountsRepository.class);
        securePassword = Mockito.mock(SecurePassword.class);
        jwtProvider = Mockito.mock(JWTProvider.class);
        accountService = new AccountServiceImpl(accountsRepository, securePassword, jwtProvider);
    }

    @Test
    void create() {
        // TODO
    }

    @Test
    void getById() {
        // TODO
    }

    @Test
    void authenticate() {
        // TODO
    }
}