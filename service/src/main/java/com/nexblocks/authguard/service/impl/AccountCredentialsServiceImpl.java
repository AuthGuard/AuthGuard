package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.passwords.SecurePassword;
import com.nexblocks.authguard.basic.passwords.SecurePasswordProvider;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.dal.model.PasswordDO;
import com.nexblocks.authguard.dal.model.UserIdentifierDO;
import com.nexblocks.authguard.dal.persistence.AccountsRepository;
import com.nexblocks.authguard.dal.persistence.CredentialsAuditRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.emb.Messages;
import com.nexblocks.authguard.service.AccountCredentialsService;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.messaging.ResetTokenMessage;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import com.nexblocks.authguard.service.util.AsyncUtils;
import com.nexblocks.authguard.service.util.CredentialsManager;
import com.nexblocks.authguard.service.util.ID;
import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class AccountCredentialsServiceImpl implements AccountCredentialsService {
    private static final Logger LOG = LoggerFactory.getLogger(AccountCredentialsServiceImpl.class);

    private static final String CREDENTIALS_CHANNEL = "credentials";
    private static final int RESET_TOKEN_SIZE = 128;
    private static final Duration TOKEN_LIFETIME = Duration.ofMinutes(30);

    private final AccountsService accountsService;
    private final AccountsRepository accountsRepository;
    private final CredentialsAuditRepository credentialsAuditRepository;
    private final AccountTokensRepository accountTokensRepository;
    private final SecurePassword securePassword;
    private final CredentialsManager credentialsManager;
    private final MessageBus messageBus;
    private final ServiceMapper serviceMapper;

    private final CryptographicRandom cryptographicRandom;

    @Inject
    public AccountCredentialsServiceImpl(final AccountsService accountsService, final AccountsRepository accountsRepository,
                                         final CredentialsAuditRepository credentialsAuditRepository,
                                         final AccountTokensRepository accountTokensRepository,
                                         final SecurePasswordProvider securePasswordProvider,
                                         final CredentialsManager credentialsManager,
                                         final MessageBus messageBus,
                                         final ServiceMapper serviceMapper) {
        this.accountsService = accountsService;
        this.accountsRepository = accountsRepository;
        this.credentialsManager = credentialsManager;
        this.credentialsAuditRepository = credentialsAuditRepository;
        this.accountTokensRepository = accountTokensRepository;
        this.securePassword = securePasswordProvider.get();
        this.messageBus = messageBus;
        this.serviceMapper = serviceMapper;

        this.cryptographicRandom = new CryptographicRandom();
    }

    @Override
    public Uni<AccountBO> updatePassword(final long id, final String plainPassword, final String domain) {
        return accountsService.getByIdUnsafe(id, domain)
                .flatMap(existing -> {
                    storeAuditAttempt(existing);

                    return credentialsManager.verifyAndHashPassword(plainPassword)
                            .flatMap(newPassword -> accountsRepository.updateUserPassword(serviceMapper.toDO(existing),
                                            PasswordDO.builder()
                                                    .password(newPassword.getPassword())
                                                    .salt(newPassword.getSalt())
                                                    .build())
                                    .map(result -> {
                                        storePasswordUpdateRecord(existing);

                                        LOG.info("Password updated. accountId={}, domain={}", id, existing.getDomain());

                                        return result;
                                    }));
                })
                .flatMap(ignored -> accountsService.getById(id, domain)
                        .flatMap(AsyncUtils::uniFromAccountOptional)
                        .map(result -> {
                            messageBus.publish(CREDENTIALS_CHANNEL, Messages.updated(result, domain));

                            return result;
                        }));
    }

    @Override
    public Uni<AccountBO> addIdentifiers(final long id, final UserIdentifierBO identifiers,
                                         final String domain) {
        return accountsService.getByIdUnsafe(id, domain)
                .flatMap(existing -> {
                    LOG.info("Add identifiers request. accountId={}, domain={}", id, existing.getDomain());

                    return accountsRepository.addUserIdentifier(serviceMapper.toDO(existing),
                            UserIdentifierDO.builder()
                                    .domain(domain)
                                    .identifier(identifiers.getIdentifier())
                                    .accountId(id)
                                    .type(UserIdentifierDO.Type.valueOf(identifiers.getType().name()))
                                    .active(true)
                                    .build());
                })
                .flatMap(ignored -> accountsService.getById(id, domain)
                        .flatMap(AsyncUtils::uniFromAccountOptional));
    }

    @Override
    public Uni<AccountBO> removeIdentifiers(final long id, final String identifier, final String domain) {
        return accountsService.getByIdUnsafe(id, domain)
                .flatMap(existing -> {
                    LOG.info("Remove identifiers request. accountId={}, domain={}", id, existing.getDomain());

                    Optional<UserIdentifierBO> identifierToUpdateOpt = existing.getIdentifiers().stream()
                            .filter(userIdentifier -> userIdentifier.getIdentifier().equals(identifier))
                            .findFirst();

                    if (identifierToUpdateOpt.isEmpty()) {
                        return Uni.createFrom().failure(new ServiceException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST,
                                "Identifier not matched"));
                    }

                    UserIdentifierBO update = identifierToUpdateOpt.get()
                            .withActive(false);

                    return accountsRepository.removeUserIdentifier(serviceMapper.toDO(existing),
                            UserIdentifierDO.builder()
                                    .id(ID.generate())
                                    .domain(domain)
                                    .identifier(identifier)
                                    .accountId(id)
                                    .type(UserIdentifierDO.Type.valueOf(identifierToUpdateOpt.get().getType().name()))
                                    .active(false)
                                    .build());
                })
                .flatMap(ignored -> accountsService.getById(id, domain)
                        .flatMap(AsyncUtils::uniFromAccountOptional));
    }

    @Override
    public Uni<AccountBO> replaceIdentifier(final long id, final String oldIdentifier,
                                            final UserIdentifierBO newIdentifier, final String domain) {
        return accountsService.getByIdUnsafe(id, domain)
                .flatMap(existing -> {
                    LOG.info("Replace identifiers request. accountId={}, domain={}", id, existing.getDomain());

                    Optional<UserIdentifierBO> matchedIdentifier = existing.getIdentifiers()
                            .stream()
                            .filter(identifier -> identifier.getIdentifier().equals(oldIdentifier))
                            .findFirst();

                    if (matchedIdentifier.isEmpty()) {
                        return Uni.createFrom().failure(new ServiceException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST, "Credentials " + id + " has no identifier " + oldIdentifier));
                    }

                    return accountsRepository.replaceIdentifierInPlace(serviceMapper.toDO(existing),
                            oldIdentifier, UserIdentifierDO.builder()
                                    .id(ID.generate())
                                    .domain(domain)
                                    .identifier(newIdentifier.getIdentifier())
                                    .accountId(id)
                                    .type(UserIdentifierDO.Type.valueOf(newIdentifier.getType().name()))
                                    .active(true)
                                    .build());
                })
                .flatMap(ignored -> accountsService.getById(id, domain)
                        .flatMap(AsyncUtils::uniFromAccountOptional));
    }

    @Override
    public Uni<PasswordResetTokenBO> generateResetToken(final String identifier, final boolean returnToken,
                                                                      final String domain) {
        return accountsService.getByIdentifier(identifier, domain)
                .flatMap(AsyncUtils::uniFromAccountOptional)
                .flatMap(account -> {
                    LOG.info("Generate password reset token request. accountId={}, domain={}", account.getId(), account.getDomain());

                    final Instant now = Instant.now();

                    final AccountTokenDO accountToken = AccountTokenDO
                            .builder()
                            .id(ID.generate())
                            .createdAt(now)
                            .token(cryptographicRandom.base64Url(RESET_TOKEN_SIZE))
                            .associatedAccountId(account.getId())
                            .expiresAt(now.plus(TOKEN_LIFETIME))
                            .build();

                    return accountTokensRepository.save(accountToken)
                            .map(persistedToken -> {
                                LOG.info("Password reset token persisted. accountId={}, domain={}, tokenId={}, expiresAt={}",
                                        account.getId(), account.getDomain(), accountToken.getId(), accountToken.getExpiresAt());

                                messageBus.publish(CREDENTIALS_CHANNEL,
                                        Messages.resetTokenGenerated(new ResetTokenMessage(account, persistedToken), domain));

                                return PasswordResetTokenBO.builder()
                                        .token(returnToken ? persistedToken.getToken() : null)
                                        .issuedAt(now.toEpochMilli() / 1000)
                                        .expiresAt(persistedToken.getExpiresAt().toEpochMilli() / 1000)
                                        .build();
                            });
                });
    }

    @Override
    public Uni<AccountBO> resetPasswordByToken(final String token, final String plainPassword, final String domain) {
        return accountTokensRepository.getByToken(token)
                .flatMap(opt -> {
                    AccountTokenDO accountToken = opt.orElseThrow(() -> new ServiceNotFoundException(ErrorCode.TOKEN_EXPIRED_OR_DOES_NOT_EXIST,
                                    "AccountDO token " + token + " does not exist"));

                    LOG.info("Password reset by token request. tokenId={}", accountToken.getId());

                    if (accountToken.getExpiresAt().isBefore(Instant.now())) {
                        throw new ServiceException(ErrorCode.EXPIRED_TOKEN, "Token " + token + " has expired");
                    }

                    return updatePassword(accountToken.getAssociatedAccountId(), plainPassword, domain);
                });
    }

    @Override
    public Uni<AccountBO> replacePassword(final String identifier,
                                          final String oldPassword,
                                          final String newPassword, final String domain) {
        return accountsService.getByIdentifierUnsafe(identifier, domain)
                .flatMap(AsyncUtils::uniFromAccountOptional)
                .flatMap(account -> {
                    LOG.info("Password replace request. accountId={}, domain={}", account.getId(), domain);

                    storeAuditAttempt(account);

                    return securePassword.verify(oldPassword, account.getHashedPassword())
                            .map(success -> {
                                if (success) {
                                    LOG.info("password replace: verified");
                                    return account;
                                }

                                LOG.info("Password mismatch in replace request. accountId={}, domain={}", account.getId(), domain);

                                throw new ServiceException(ErrorCode.PASSWORDS_DO_NOT_MATCH, "Passwords do not match");
                            });
                })
                .flatMap(account -> {
                    LOG.info("password replace: Got credentials");

                    return credentialsManager.verifyAndHashPassword(newPassword)
                            .flatMap(newHashedPassword -> {
                                return accountsRepository.updateUserPassword(serviceMapper.toDO(account),
                                                PasswordDO.builder()
                                                        .password(newHashedPassword.getPassword())
                                                        .salt(newHashedPassword.getSalt())
                                                        .build())
                                        .map(result -> {
                                            storePasswordUpdateRecord(account);

                                            LOG.info("Password updated. accountId={}, domain={}", account.getId(),
                                                    account.getDomain());

                                            return result;
                                        });
                            })
                            .flatMap(ignored -> accountsService.getById(account.getId(), domain)
                                    .flatMap(AsyncUtils::uniFromAccountOptional));
                });
    }

    private Uni<AccountBO> doUpdate(final AccountBO existing, final AccountBO updated, final String domain) {
        LOG.info("Account credentials update. accountId={}, domain={}", existing.getId(), existing.getDomain());
        storeAuditAttempt(existing);

        return accountsService.update(updated, domain)
                .map(persisted -> persisted.map(c -> {
                            messageBus.publish(CREDENTIALS_CHANNEL, Messages.updated(c, domain));

                            return c;
                        })
                        .map(credentialsManager::removeSensitiveInformation)
                        .orElseThrow(() -> new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, "Account does not exist")));
    }

    private void storeIdentifierUpdateRecord(final AccountBO credentials, final UserIdentifierBO update,
                                             final CredentialsAudit.Action action) {
        final CredentialsAuditBO audit = CredentialsAuditBO.builder()
                .id(ID.generate())
                .credentialsId(credentials.getId())
                .action(action)
                .identifier(update.getIdentifier())
                .build();

        LOG.info("Storing identifier update audit record. accountId={}, auditRecordId={}, action={}",
                credentials.getId(), audit.getId(), action);

        credentialsAuditRepository.save(serviceMapper.toDO(audit));
    }

    private void storePasswordUpdateRecord(final AccountBO credentials) {
        final CredentialsAuditBO audit = CredentialsAuditBO.builder()
                .id(ID.generate())
                .credentialsId(credentials.getId())
                .action(CredentialsAudit.Action.UPDATED)
                .password(credentials.getHashedPassword())
                .build();

        LOG.info("Storing password update audit record. accountId={}, auditRecordId={}, action={}",
                credentials.getId(), audit.getId(), audit.getAction());

        credentialsAuditRepository.save(serviceMapper.toDO(audit))
                .subscribe()
                .with(ignored -> {});
    }

    private void storeAuditAttempt(final AccountBO credentials) {
        final CredentialsAuditBO audit = CredentialsAuditBO.builder()
                .id(ID.generate())
                .credentialsId(credentials.getId())
                .action(CredentialsAudit.Action.ATTEMPT)
                .build();

        LOG.info("Storing generic credentials update audit record. accountId={}, auditRecordId={}, action={}",
                credentials.getId(), audit.getId(), audit.getAction());

        credentialsAuditRepository.save(serviceMapper.toDO(audit));
    }
}
