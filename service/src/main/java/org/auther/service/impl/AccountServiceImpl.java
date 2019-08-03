package org.auther.service.impl;

import org.auther.dal.AccountsRepository;
import org.auther.dal.model.AccountDO;
import org.auther.service.AccountService;
import org.auther.service.JWTProvider;
import org.auther.service.SecurePassword;
import org.auther.service.exceptions.ServiceAuthorizationException;
import org.auther.service.exceptions.ServiceException;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokensBO;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public class AccountServiceImpl implements AccountService {
    private final AccountsRepository accountsRepository;
    private final SecurePassword securePassword;
    private final JWTProvider jwtProvider;

    public AccountServiceImpl(final AccountsRepository accountsRepository, final SecurePassword securePassword,
                              final JWTProvider jwtProvider) {
        this.accountsRepository = accountsRepository;
        this.securePassword = securePassword;
        this.jwtProvider = jwtProvider;
    }

    @Override
    public AccountBO create(final AccountBO account) {
        final String hashedPassword = securePassword.hash(account.getPassword());

        return Optional.of(Mappers.getMapper().map(account, AccountDO.class))
                .map(accountDO -> accountDO
                        .withId(UUID.randomUUID().toString())
                        .withPassword(hashedPassword)
                )
                .map(accountsRepository::save)
                .map(createdAccount -> Mappers.getMapper().map(createdAccount, AccountBO.class))
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    public Optional<AccountBO> getById(final String accountId) {
        return accountsRepository.getById(accountId)
                .map(accountDO -> Mappers.getMapper().map(accountDO, AccountBO.class))
                .map(accountBO -> accountBO.withPassword("")); // no matter what, there's no reason for it to leave the service
    }

    @Override
    public Optional<TokensBO> authenticate(final String authorization) {
        final String[] parts = parseAuthorization(authorization);

        if (parts[0].equals("Basic")) {
            return handleBasicAuthentication(parts[1])
                    .map(jwtProvider::generateToken);
        } else {
            throw new ServiceException("Unsupported authorization scheme");
        }
    }

    private Optional<AccountBO> handleBasicAuthentication(final String base64Credentials) {
        final String[] decoded = new String(Base64.getDecoder().decode(base64Credentials)).split(":");

        if (decoded.length != 2) {
            throw new ServiceException("Invalid format for basic authentication");
        }

        final String username =  decoded[0];
        final String password = decoded[1];

        final Optional<AccountBO> account = accountsRepository.findByUsername(username)
                .map(accountDO -> Mappers.getMapper().map(accountDO, AccountBO.class));

        if (account.isPresent()) {
            if (!securePassword.verify(password, account.get().getPassword())) {
                throw new ServiceAuthorizationException("Passwords don't match");
            }
        }

        return account;
    }

    private String[] parseAuthorization(final String authorization) {
        final String[] parts = authorization.split("\\s");

        if (parts.length != 2) {
            throw new ServiceException("Invalid format for authorization value");
        }

        return parts;
    }
}
