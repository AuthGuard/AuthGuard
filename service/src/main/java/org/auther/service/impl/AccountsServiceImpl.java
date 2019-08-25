package org.auther.service.impl;

import com.google.inject.Inject;
import org.auther.dal.AccountsRepository;
import org.auther.service.AccountsService;
import org.auther.service.JWTProvider;
import org.auther.service.SecurePassword;
import org.auther.service.exceptions.ServiceAuthorizationException;
import org.auther.service.exceptions.ServiceException;
import org.auther.service.exceptions.ServiceNotFoundException;
import org.auther.service.model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AccountsServiceImpl implements AccountsService {
    private final AccountsRepository accountsRepository;
    private final SecurePassword securePassword;
    private final JWTProvider jwtProvider;
    private final ServiceMapper serviceMapper;

    @Inject
    public AccountsServiceImpl(final AccountsRepository accountsRepository, final SecurePassword securePassword,
                               final JWTProvider jwtProvider, final ServiceMapper serviceMapper) {
        this.accountsRepository = accountsRepository;
        this.securePassword = securePassword;
        this.jwtProvider = jwtProvider;
        this.serviceMapper = serviceMapper;
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
                .map(this::removeSensitiveInformation)
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    public Optional<AccountBO> getById(final String accountId) {
        return accountsRepository.getById(accountId)
                .map(serviceMapper::toBO)
                .map(this::removeSensitiveInformation); // no matter what, there's no reason for it to leave the service
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

        return removeSensitiveInformation(updated);
    }

    @Override
    public AccountBO revokePermissions(final String accountId, final List<PermissionBO> permissions) {
        final AccountBO account = accountsRepository.getById(accountId)
                .map(serviceMapper::toBO)
                .orElseThrow(ServiceNotFoundException::new);

        final List<PermissionBO> filteredPermissions = account.getPermissions().stream()
                .filter(permission -> !permissions.contains(permission))
                .collect(Collectors.toList());

        final AccountBO updated = account.withPermissions(filteredPermissions);

        accountsRepository.update(serviceMapper.toDO(updated));

        return removeSensitiveInformation(updated);
    }

    @Override
    public AccountBO grantRoles(final String accountId, final List<String> roles) {
        final AccountBO account = accountsRepository.getById(accountId)
                .map(serviceMapper::toBO)
                .orElseThrow(ServiceNotFoundException::new);

        final List<String> combinedRoles = Stream.concat(account.getRoles().stream(), roles.stream())
                .distinct()
                .collect(Collectors.toList());

        final AccountBO updated = account.withRoles(combinedRoles);

        return accountsRepository.update(serviceMapper.toDO(updated))
                .map(serviceMapper::toBO)
                .map(this::removeSensitiveInformation)
                .orElseThrow(IllegalStateException::new);
    }

    @Override
    public AccountBO revokeRoles(final String accountId, final List<String> roles) {
        final AccountBO account = accountsRepository.getById(accountId)
                .map(serviceMapper::toBO)
                .orElseThrow(ServiceNotFoundException::new);

        final List<String> filteredRoles = account.getRoles().stream()
                .filter(role -> !roles.contains(role))
                .collect(Collectors.toList());

        final AccountBO updated = account.withRoles(filteredRoles);

        return accountsRepository.update(serviceMapper.toDO(updated))
                .map(serviceMapper::toBO)
                .map(this::removeSensitiveInformation)
                .orElseThrow(IllegalStateException::new);
    }

    private AccountBO removeSensitiveInformation(final AccountBO account) {
        return account.withPlainPassword(null)
                .withHashedPassword(null);
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
