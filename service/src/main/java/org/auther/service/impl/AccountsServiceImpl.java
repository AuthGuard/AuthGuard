package org.auther.service.impl;

import org.auther.dal.AccountsRepository;
import org.auther.service.AccountsService;
import org.auther.service.JWTProvider;
import org.auther.service.SecurePassword;
import org.auther.service.exceptions.ServiceAuthorizationException;
import org.auther.service.exceptions.ServiceException;
import org.auther.service.exceptions.ServiceNotFoundException;
import org.auther.service.model.AccountBO;
import org.auther.service.model.HashedPasswordBO;
import org.auther.service.model.PermissionBO;
import org.auther.service.model.TokensBO;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AccountsServiceImpl implements AccountsService {
    private final AccountsRepository accountsRepository;
    private final SecurePassword securePassword;
    private final JWTProvider jwtProvider;
    private final ServiceMapper serviceMapper;

    public AccountsServiceImpl(final AccountsRepository accountsRepository, final SecurePassword securePassword,
                               final JWTProvider jwtProvider) {
        this.accountsRepository = accountsRepository;
        this.securePassword = securePassword;
        this.jwtProvider = jwtProvider;

        this.serviceMapper = ServiceMapper.INSTANCE;
    }

    @Override
    public AccountBO create(final AccountBO account) {
        Objects.requireNonNull(account.getHashedPassword());

        final HashedPasswordBO hashedPassword = securePassword.hash(account.getPlainPassword());

        return Optional.of(account)
                .map(accountBO -> accountBO
                        .withId(UUID.randomUUID().toString())
                        .withHashedPassword(hashedPassword)
                )
                .map(serviceMapper::toDO)
                .map(accountsRepository::save)
                .map(serviceMapper::toBO)
                .map(accountBO -> accountBO
                        .withPlainPassword(null)
                        .withHashedPassword(null)
                )
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    public Optional<AccountBO> getById(final String accountId) {
        return accountsRepository.getById(accountId)
                .map(serviceMapper::toBO)
                .map(accountBO -> accountBO.withPlainPassword(null).withHashedPassword(null)); // no matter what, there's no reason for it to leave the service
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

    @Override
    public AccountBO grantPermissions(final String accountId, final List<PermissionBO> permissions) {
        final AccountBO account = accountsRepository.getById(accountId)
                .map(serviceMapper::toBO)
                .orElseThrow(ServiceNotFoundException::new);

        // TODO verify permissions using permissions service

        final List<PermissionBO> combinedPermissions = Stream.concat(account.getPermissions().stream(), permissions.stream())
                .distinct()
                .collect(Collectors.toList());

        final AccountBO updated = account.withPermissions(combinedPermissions);

        accountsRepository.update(serviceMapper.toDO(updated));

        return updated
                .withHashedPassword(null)
                .withPlainPassword(null);
    }

    @Override
    public AccountBO revokePermissions(final String accountId, final List<PermissionBO> permissions) {
        final AccountBO account = accountsRepository.getById(accountId)
                .map(serviceMapper::toBO)
                .orElseThrow(ServiceNotFoundException::new);

        final List<PermissionBO> combinedPermissions = account.getPermissions().stream()
                .filter(permission -> !permissions.contains(permission))
                .collect(Collectors.toList());

        final AccountBO updated = account.withPermissions(combinedPermissions);

        accountsRepository.update(serviceMapper.toDO(updated));

        return updated
                .withHashedPassword(null)
                .withPlainPassword(null);
    }

    private Optional<AccountBO> handleBasicAuthentication(final String base64Credentials) {
        final String[] decoded = new String(Base64.getDecoder().decode(base64Credentials)).split(":");

        if (decoded.length != 2) {
            throw new ServiceException("Invalid format for basic authentication");
        }

        final String username =  decoded[0];
        final String password = decoded[1];

        final Optional<AccountBO> account = accountsRepository.findByUsername(username)
                .map(serviceMapper::toBO);

        if (account.isPresent()) {
            if (!securePassword.verify(password, account.get().getHashedPassword())) {
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
