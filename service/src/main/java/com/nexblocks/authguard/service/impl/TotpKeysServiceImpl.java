package com.nexblocks.authguard.service.impl;

import com.atlassian.onetime.model.EmailAddress;
import com.atlassian.onetime.model.Issuer;
import com.atlassian.onetime.model.TOTPSecret;
import com.atlassian.onetime.service.DefaultTOTPService;
import com.atlassian.onetime.service.TOTPService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.crypto.ChaCha20Encryptor;
import com.nexblocks.authguard.crypto.KeyLoader;
import com.nexblocks.authguard.dal.model.TotpKeyDO;
import com.nexblocks.authguard.dal.persistence.TotpKeysRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.TotpKeysService;
import com.nexblocks.authguard.service.config.TotpAuthenticatorsConfig;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.Account;
import com.nexblocks.authguard.service.model.TotpKeyBO;
import com.nexblocks.authguard.service.model.UserIdentifierBO;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import org.bouncycastle.util.encoders.Base32;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TotpKeysServiceImpl implements TotpKeysService {
    private static final int KEY_SIZE = 20;
    private static final String TOTP_KEYS_CHANNEL = "totp_keys";

    private final TotpKeysRepository repository;
    private final AccountsService accountsService;
    private final ServiceMapper serviceMapper;
    private final PersistenceService<TotpKeyBO, TotpKeyDO, TotpKeysRepository> persistenceService;
    private final CryptographicRandom cryptographicRandom;
    private final TotpAuthenticatorsConfig config;
    private final byte[] encryptionKey;
    private final TOTPService totpService;

    @Inject
    public TotpKeysServiceImpl(final TotpKeysRepository repository,
                               final AccountsService accountsService,
                               final ServiceMapper serviceMapper,
                               final MessageBus messageBus,
                               final @Named("totpAuthenticators") ConfigContext config) {
        this.repository = repository;
        this.accountsService = accountsService;
        this.serviceMapper = serviceMapper;

        this.persistenceService = new PersistenceService<>(repository, messageBus,
                serviceMapper::toDO, serviceMapper::toBO, TOTP_KEYS_CHANNEL);
        this.cryptographicRandom = new CryptographicRandom();
        this.config = config.asConfigBean(TotpAuthenticatorsConfig.class);
        this.encryptionKey = Base64.getDecoder()
                .decode(KeyLoader.readTexFileOrValue(this.config.getEncryptionKey()));
        this.totpService = new DefaultTOTPService();
    }

    @Override
    public CompletableFuture<TotpKeyBO> generate(final long accountId, final String domain,
                                                 final String authenticator) {
        return accountsService.getById(accountId, domain)
                .thenCompose(opt -> {
                    if (opt.isEmpty()) {
                        return CompletableFuture.failedFuture(new ServiceException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                                "Account does not exist"));
                    }

                    return generate(opt.get(), authenticator);
                });
    }

    private CompletableFuture<TotpKeyBO> generate(final Account account, final String authenticator) {
        return repository.findByAccountId(account.getDomain(), account.getId())
                .thenCompose(existing -> {
                    if (!existing.isEmpty()) {
                        return CompletableFuture.failedFuture(new ServiceException(ErrorCode.TOTP_ALREADY_EXISTS,
                                "Account already has an active key"));
                    }

                    byte[] nonce = cryptographicRandom.bytes(12);
                    byte[] key = cryptographicRandom.bytes(KEY_SIZE);
                    String base32Key = Base32.toBase32String(key);

                    byte[] encryptedKey = ChaCha20Encryptor.encrypt(key, encryptionKey, nonce);
                    String qrCode = config.generateQrCode() ? generateQrCode(base32Key, account) : "";

                    // we store the encrypted version but return the plain one
                    return persistenceService.create(TotpKeyBO.builder()
                                    .domain(account.getDomain())
                                    .accountId(account.getId())
                                    .authenticator(authenticator)
                                    .key(encryptedKey)
                                    .nonce(nonce)
                                    .build())
                            .thenApply(persisted -> TotpKeyBO.builder()
                                    .from(persisted)
                                    .qrCode(qrCode)
                                    .key(key)
                                    .build());
                });
    }

    @Override
    public CompletableFuture<List<TotpKeyBO>> getByAccountId(final long accountId, final String domain) {
        return repository.findByAccountId(domain, accountId)
                .thenApply(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<Optional<TotpKeyBO>> getByAccountIdDecrypted(final long accountId, final String domain) {
        return repository.findByAccountId(domain, accountId)
                .thenApply(list -> list.stream().findFirst())
                .thenApply(opt -> opt.map(totpKeyDO -> {
                    byte[] encrypted = totpKeyDO.getEncryptedKey();
                    byte[] decrypted = ChaCha20Encryptor.decrypt(encrypted, encryptionKey,
                            totpKeyDO.getNonce());

                    return serviceMapper.toBO(totpKeyDO)
                            .withKey(decrypted);
                }));
    }

    @Override
    public CompletableFuture<Optional<TotpKeyBO>> getById(final long id, final String domain) {
        return persistenceService.getById(id, domain);
    }

    @Override
    public CompletableFuture<Optional<TotpKeyBO>> delete(final long id, final String domain) {
        return persistenceService.delete(id);
    }

    private String generateQrCode(final String plainKey, final Account account) {
        Optional<String> userId = account.getIdentifiers().stream()
                .filter(identifier -> Objects.equals(identifier.getType(), config.getQrUserIdentifierType()))
                .map(UserIdentifierBO::getIdentifier)
                .findFirst();

        if (userId.isEmpty()) {
            throw new ServiceException(ErrorCode.IDENTIFIER_DOES_NOT_EXIST,
                    "Account " + account.getId() + " has no identifier of type " + config.getQrUserIdentifierType());
        }

        return totpService.generateTOTPUrl(TOTPSecret.Companion.fromBase32EncodedString(plainKey),
                new EmailAddress(userId.get()),
                new Issuer(config.getQrIssuer())).toString();
    }
}
