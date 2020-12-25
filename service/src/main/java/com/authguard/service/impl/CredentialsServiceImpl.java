package com.authguard.service.impl;

import com.authguard.basic.passwords.PasswordValidator;
import com.authguard.basic.passwords.SecurePassword;
import com.authguard.basic.passwords.ServiceInvalidPasswordException;
import com.authguard.basic.passwords.Violation;
import com.authguard.dal.persistence.CredentialsAuditRepository;
import com.authguard.dal.persistence.CredentialsRepository;
import com.authguard.dal.model.CredentialsDO;
import com.authguard.emb.MessageBus;
import com.authguard.emb.Messages;
import com.authguard.service.AccountsService;
import com.authguard.service.CredentialsService;
import com.authguard.service.IdempotencyService;
import com.authguard.service.exceptions.ServiceConflictException;
import com.authguard.service.exceptions.ServiceException;
import com.authguard.service.exceptions.ServiceNotFoundException;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.model.*;
import com.google.inject.Inject;

import java.util.*;
import java.util.stream.Collectors;

public class CredentialsServiceImpl implements CredentialsService {
    private static final String CREDENTIALS_CHANNEL = "credentials";

    private final AccountsService accountsService;
    private final IdempotencyService idempotencyService;
    private final CredentialsRepository credentialsRepository;
    private final CredentialsAuditRepository credentialsAuditRepository;
    private final SecurePassword securePassword;
    private final PasswordValidator passwordValidator;
    private final MessageBus messageBus;
    private final ServiceMapper serviceMapper;

    @Inject
    public CredentialsServiceImpl(final AccountsService accountsService,
                                  final IdempotencyService idempotencyService,
                                  final CredentialsRepository credentialsRepository,
                                  final CredentialsAuditRepository credentialsAuditRepository,
                                  final SecurePassword securePassword,
                                  final PasswordValidator passwordValidator,
                                  final MessageBus messageBus,
                                  final ServiceMapper serviceMapper) {
        this.accountsService = accountsService;
        this.idempotencyService = idempotencyService;
        this.credentialsRepository = credentialsRepository;
        this.credentialsAuditRepository = credentialsAuditRepository;
        this.securePassword = securePassword;
        this.passwordValidator = passwordValidator;
        this.messageBus = messageBus;
        this.serviceMapper = serviceMapper;
    }

    @Override
    public CredentialsBO create(final CredentialsBO credentials, final RequestContextBO requestContext) {
        return idempotencyService.performOperation(() -> doCreate(credentials), requestContext.getIdempotentKey(), credentials.getEntityType())
                .join();
    }

    private CredentialsBO doCreate(final CredentialsBO credentials) {
        ensureAccountExists(credentials.getAccountId());

        final HashedPasswordBO hashedPassword = verifyAndHashPassword(credentials.getPlainPassword());
        final CredentialsBO credentialsHashedPassword = CredentialsBO.builder()
                .from(credentials)
                .hashedPassword(hashedPassword)
                .id(UUID.randomUUID().toString())
                .build();

        final CredentialsDO credentialsDO = serviceMapper.toDO(credentialsHashedPassword);

        return credentialsRepository.save(credentialsDO)
                .thenApply(serviceMapper::toBO)
                .thenApply(this::removeSensitiveInformation)
                .thenApply(created -> {
                    messageBus.publish(CREDENTIALS_CHANNEL, Messages.created(created));
                    return created;
                })
                .join();
    }

    @Override
    public Optional<CredentialsBO> getById(final String id) {
        return credentialsRepository.getById(id)
                .thenApply(optional -> optional.map(serviceMapper::toBO).map(this::removeSensitiveInformation))
                .join();
    }

    @Override
    public Optional<CredentialsBO> getByUsername(final String username) {
        return credentialsRepository.findByIdentifier(username)
                .thenApply(optional -> optional.map(serviceMapper::toBO).map(this::removeSensitiveInformation))
                .join();
    }

    @Override
    public Optional<CredentialsBO> getByUsernameUnsafe(final String username) {
        return credentialsRepository.findByIdentifier(username)
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join();
    }

    @Override
    public Optional<CredentialsBO> update(final CredentialsBO credentials) {
        return doUpdate(credentials.withPlainPassword(null).withHashedPassword(null), false);
    }

    @Override
    public Optional<CredentialsBO> updatePassword(final CredentialsBO credentials) {
        final HashedPasswordBO newPassword = verifyAndHashPassword(credentials.getPlainPassword());
        final CredentialsBO update = credentials.withHashedPassword(newPassword);

        return doUpdate(update, true);
    }

    @Override
    public Optional<CredentialsBO> addIdentifiers(final String id, final List<UserIdentifierBO> identifiers) {
        final CredentialsBO existing = getById(id)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST, "No credentials with ID " + id));

        final Set<String> existingIdentifiers = existing.getIdentifiers().stream()
                .map(UserIdentifierBO::getIdentifier)
                .collect(Collectors.toSet());

        final List<UserIdentifierBO> combined = new ArrayList<>(existing.getIdentifiers());

        for (final UserIdentifierBO identifier : identifiers) {
            if (existingIdentifiers.contains(identifier.getIdentifier())) {
                throw new ServiceConflictException(ErrorCode.IDENTIFIER_ALREADY_EXISTS, "Duplicate identifier for " + id);
            }

            combined.add(identifier.withActive(true));
        }

        final CredentialsBO updated = CredentialsBO.builder().from(existing)
                .identifiers(combined)
                .build();

        return update(updated);
    }

    @Override
    public Optional<CredentialsBO> removeIdentifiers(final String id, final List<String> identifiers) {
        final CredentialsBO existing = getById(id)
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST, "No credentials with ID " + id));

        final List<UserIdentifierBO> updatedIdentifiers = existing.getIdentifiers().stream()
                .map(identifier -> {
                    if (identifiers.contains(identifier.getIdentifier())) {
                        return identifier.withActive(false);
                    }

                    return identifier;
                })
                .collect(Collectors.toList());

        final CredentialsBO updated = CredentialsBO.builder().from(existing)
                .identifiers(updatedIdentifiers)
                .build();

        return update(updated);
    }

    private Optional<CredentialsBO> doUpdate(final CredentialsBO credentials, boolean storePasswordAudit) {
        final CredentialsBO existing = credentialsRepository.getById(credentials.getId())
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join()
                .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.CREDENTIALS_DOES_NOT_EXIST, "No credentials with ID "
                        + credentials.getId() + " was found"));

        final CredentialsBO update = credentials.getHashedPassword() == null ?
                credentials.withHashedPassword(existing.getHashedPassword()) : credentials;

        // regardless of whether storePasswordAudit is true or not, we don't need the password in attempt stage
        storeAuditRecord(removeSensitiveInformation(existing), removeSensitiveInformation(credentials),
                CredentialsAudit.Action.ATTEMPT);

        return credentialsRepository.update(serviceMapper.toDO(update))
                .thenApply(optional -> optional.map(serviceMapper::toBO))
                .join()
                .map(c -> {
                    if (storePasswordAudit) {
                        storeAuditRecord(existing, credentials, CredentialsAudit.Action.UPDATED);
                    } else {
                        storeAuditRecord(removeSensitiveInformation(existing), removeSensitiveInformation(credentials),
                                CredentialsAudit.Action.UPDATED);
                    }
                    return c;
                })
                .map(this::removeSensitiveInformation)
                .map(updated -> {
                    messageBus.publish(CREDENTIALS_CHANNEL, Messages.updated(updated));
                    return updated;
                });
    }

    @Override
    public Optional<CredentialsBO> delete(final String id) {
        return credentialsRepository.delete(id)
                .thenApply(optional -> optional
                        .map(serviceMapper::toBO)
                        .map(this::removeSensitiveInformation)
                        .map(deleted -> {
                            messageBus.publish(CREDENTIALS_CHANNEL, Messages.deleted(deleted));
                            return deleted;
                        })
                )
                .join();
    }

    private HashedPasswordBO verifyAndHashPassword(final String plain) {
        final List<Violation> passwordViolations = passwordValidator.findViolations(plain);

        if (!passwordViolations.isEmpty()) {
            throw new ServiceInvalidPasswordException(passwordViolations);
        }

        return securePassword.hash(plain);
    }

    private CredentialsBO removeSensitiveInformation(final CredentialsBO credentials) {
        return credentials.withPlainPassword(null)
                .withHashedPassword(null);
    }

    private void storeAuditRecord(final CredentialsBO credentials, final CredentialsBO update,
                                  final CredentialsAudit.Action action) {
        final CredentialsAuditBO audit = CredentialsAuditBO.builder()
                .id("")
                .credentialsId(credentials.getId())
                .action(action)
                .before(credentials)
                .after(update)
                .password(credentials.getHashedPassword())
                .build();

        credentialsAuditRepository.save(serviceMapper.toDO(audit));
    }

    private void ensureAccountExists(final String accountId) {
        if (accountsService.getById(accountId).isEmpty()) {
            throw new ServiceException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "No account with ID " + accountId + " exists");
        }
    }
}
