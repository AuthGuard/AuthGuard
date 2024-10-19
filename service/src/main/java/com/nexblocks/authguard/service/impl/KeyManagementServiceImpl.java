package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.crypto.*;
import com.nexblocks.authguard.crypto.generators.*;
import com.nexblocks.authguard.dal.model.CryptoKeyDO;
import com.nexblocks.authguard.dal.persistence.CryptoKeysRepository;
import com.nexblocks.authguard.dal.persistence.Page;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.KeyManagementService;
import com.nexblocks.authguard.service.config.CryptoKeyConfig;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.EphemeralKeyBO;
import com.nexblocks.authguard.service.model.PersistedKeyBO;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class KeyManagementServiceImpl implements KeyManagementService {
    private static final Logger LOG = LoggerFactory.getLogger(KeyManagementServiceImpl.class);

    private static final int PAGE_SIZE = 100;
    private static final Instant DEFAULT_CURSOR = Instant.MAX;
    private static final String CRYPTO_KEYS_CHANNEL = "crypto_keys";

    private final CryptoKeysRepository repository;
    private final AccountsService accountsService;
    private final ApplicationsService applicationsService;
    private final CryptographicRandom cryptographicRandom;
    private final ServiceMapper serviceMapper;
    private final PersistenceService<PersistedKeyBO, CryptoKeyDO, CryptoKeysRepository> persistenceService;
    private final CryptoKeyConfig config;
    private final byte[] encryptionKey;

    @Inject
    public KeyManagementServiceImpl(final AccountsService accountsService,
                                    final ApplicationsService applicationsService,
                                    final ServiceMapper serviceMapper,
                                    final CryptoKeysRepository cryptoKeysRepository,
                                    final MessageBus messageBus,
                                    final @Named("cryptographic_keys") ConfigContext cryptoKeysConfigContext) {
        this.repository = cryptoKeysRepository;
        this.accountsService = accountsService;
        this.applicationsService = applicationsService;
        this.serviceMapper = serviceMapper;

        this.config = cryptoKeysConfigContext.asConfigBean(CryptoKeyConfig.class);
        this.encryptionKey = Base64.getDecoder().decode(KeyLoader.readTexFileOrValue(this.config.getEncryptionKey()));

        this.cryptographicRandom = new CryptographicRandom();
        this.persistenceService = new PersistenceService<>(cryptoKeysRepository, messageBus,
                serviceMapper::toDO, serviceMapper::toBO, CRYPTO_KEYS_CHANNEL);
    }

    @Override
    public CompletableFuture<PersistedKeyBO> create(final PersistedKeyBO key) {
        CompletableFuture<PersistedKeyBO> createFuture;

        if (key.getAccountId() != null) {
            createFuture = accountsService.getById(key.getAccountId(), key.getDomain())
                    .thenCompose(opt -> {
                        if (opt.isEmpty()) {
                            throw new ServiceNotFoundException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                                    "No account with ID " + key.getAccountId() + " exists");
                        }

                        return persistenceService.create(encrypt(key));
                    });
        } else if (key.getAppId() != null) {
            createFuture = applicationsService.getById(key.getAppId(), key.getDomain())
                    .thenCompose(opt -> {
                        if (opt.isEmpty()) {
                            throw new ServiceNotFoundException(ErrorCode.APP_DOES_NOT_EXIST,
                                    "No application with ID " + key.getAccountId() + " exists");
                        }

                        return persistenceService.create(encrypt(key));
                    });
        } else {
            createFuture = persistenceService.create(encrypt(key));
        }

        return createFuture
                .thenApply(persisted -> PersistedKeyBO.builder()
                        .from(persisted)
                        .privateKey(key.getPrivateKey())
                        .build()); // store encrypted, return plain
    }

    @Override
    public CompletableFuture<Optional<PersistedKeyBO>> getById(final long id, final String domain) {
        return persistenceService.getById(id, domain);
    }

    @Override
    public CompletableFuture<Optional<PersistedKeyBO>> update(final PersistedKeyBO entity, final String domain) {
        throw new UnsupportedOperationException("Cryptographic keys cannot be updated");
    }

    @Override
    public CompletableFuture<Optional<PersistedKeyBO>> delete(final long id, final String domain) {
        LOG.info("Key delete request. accountId={}", id);

        return persistenceService.delete(id);
    }

    @Override
    public EphemeralKeyBO generate(final String algorithm, final int size) {
        String algorithmUpper = algorithm.toUpperCase();

        if (Objects.equals(algorithmUpper, "AES")) {
            return generate(Algorithms.aes, new AesParameters(size));
        }

        if (Objects.equals(algorithmUpper, "RSA")) {
            return generate(Algorithms.rsa, new RsaParameters(size));
        }

        if (Objects.equals(algorithmUpper, "EC_SECP128K1")) {
            return generate(Algorithms.ecSecp256k1, new EcSecp256k1Parameters(size));
        }

        if (Objects.equals(algorithmUpper, "CHACHA20")) {
            return generate(Algorithms.chaCha20, new ChaCha20Parameters());
        }

        throw new ServiceException(ErrorCode.CRYPTO_INVALID_ALGO, "Algorithm " + algorithm +
                " is not supported");
    }

    @Override
    public CompletableFuture<Optional<PersistedKeyBO>> getDecrypted(final long id, final String domain, final String passcode) {
        return getById(id, domain)
                .thenApply(opt -> opt.map(key -> {
                    byte[] nonce;

                    if (key.isPasscodeProtected()) {
                        if (passcode == null) {
                            throw new ServiceException(ErrorCode.CRYPTO_MISSING_PASSCODE,
                                    "Passcode protection was enabled for the key but no passcode was given");
                        }

                        nonce = remixNonce(key.getNonce(), passcode);
                        byte[] passcodeCheck = ChaCha20Encryptor.decrypt(Base64.getDecoder().decode(key.getPasscodeCheckEncrypted()),
                                encryptionKey, nonce);

                        if (!Arrays.equals(passcodeCheck, key.getPasscodeCheckPlain().getBytes(StandardCharsets.UTF_8))) {
                            throw new ServiceException(ErrorCode.CRYPTO_INVALID_PASSCODE,
                                    "Passcode check failed");
                        }
                    } else {
                        nonce = key.getNonce();
                    }

                    byte[] encryptedPrivateKey = Base64.getDecoder().decode(key.getPrivateKey());
                    byte[] decryptedPrivateKey = ChaCha20Encryptor.decrypt(encryptedPrivateKey, encryptionKey, nonce);

                    return PersistedKeyBO.builder()
                            .from(key)
                            .privateKey(Base64.getEncoder().encodeToString(decryptedPrivateKey))
                            .nonce()
                            .build();
                }));
    }

    @Override
    public CompletableFuture<List<PersistedKeyBO>> getByDomain(final String domain, final Instant cursor) {
        return repository.findByDomain(domain, Page.of(cursor, PAGE_SIZE, DEFAULT_CURSOR))
                .thenApply(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<List<PersistedKeyBO>> getByAccountId(final String domain, final long accountId,
                                                                  final Instant cursor) {
        return repository.findByAccountId(domain, accountId, Page.of(cursor, PAGE_SIZE, DEFAULT_CURSOR))
                .thenApply(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<List<PersistedKeyBO>> getByAppId(final String domain, final long appId, final Instant cursor) {
        return repository.findByAppId(domain, appId, Page.of(cursor, PAGE_SIZE, DEFAULT_CURSOR))
                .thenApply(list -> list.stream()
                        .map(serviceMapper::toBO)
                        .collect(Collectors.toList()));

    }

    private void verifySize(final AlgorithmDetails<?> algorithmDetails, final int size) {
        if (!algorithmDetails.getAllowedSizes().contains(size)) {
            throw new ServiceException(ErrorCode.CRYPTO_INVALID_SIZE, "Algorithm " + algorithmDetails.getName() +
                    " does not support size " + size);
        }
    }

    private <T extends GeneratorParameters> EphemeralKeyBO generate(final AlgorithmDetails<T> algorithm,
                                                                    final T parameters) {
        verifySize(algorithm, parameters.getSize());

        GeneratorResult result = algorithm.getGenerator().generate(parameters);

        return fromGeneratorResult(algorithm.getName(), parameters.getSize(), result);
    }

    private EphemeralKeyBO fromGeneratorResult(final String algorithm, final int size,
                                               final GeneratorResult result) {
        return EphemeralKeyBO.builder()
                .algorithm(algorithm)
                .size(size)
                .privateKey(toBase64(result.getPrivateKey()))
                .publicKey(toBase64(result.getPublicKey()))
                .build();
    }

    private String toBase64(final byte[] raw) {
        return raw == null ? null : Base64.getEncoder().encodeToString(raw);
    }

    private PersistedKeyBO encrypt(final PersistedKeyBO key) {
        byte[] nonce = cryptographicRandom.bytes(12);

        PersistedKeyBO.Builder persistedKeyBuilder = PersistedKeyBO.builder()
                .from(key)
                .nonce(nonce)
                .version(config.getVersion());

        if (key.isPasscodeProtected()) {
            if (key.getPasscode() == null) {
                throw new ServiceException(ErrorCode.CRYPTO_INVALID_PARAMS,
                        "Passcode protection was enabled for the key but no passcode was given");
            }

            encryptWithPasscode(persistedKeyBuilder, key, nonce);
        } else {
            byte[] privateKeyRaw = Base64.getDecoder().decode(key.getPrivateKey());
            byte[] encryptedPrivateKey = ChaCha20Encryptor.encrypt(privateKeyRaw, encryptionKey, nonce);

            persistedKeyBuilder.privateKey(Base64.getEncoder().encodeToString(encryptedPrivateKey));
        }

        return persistedKeyBuilder.build();
    }

    private void encryptWithPasscode(final PersistedKeyBO.Builder encryptedKeyBuilder,
                                     final PersistedKeyBO plainKey,
                                     final byte[] nonce) {
        byte[] remixedNonce = remixNonce(nonce, plainKey.getPasscode());

        String passcodeCheckPlain = RandomStringUtils.random(5);
        byte[] passcodeCheckPlainBytes = passcodeCheckPlain.getBytes(StandardCharsets.UTF_8);
        byte[] passcodeCheckEncryptedBytes = ChaCha20Encryptor.encrypt(passcodeCheckPlainBytes, encryptionKey, remixedNonce);

        byte[] privateKeyRaw = Base64.getDecoder().decode(plainKey.getPrivateKey());
        byte[] encryptedPrivateKey = ChaCha20Encryptor.encrypt(privateKeyRaw, encryptionKey, remixedNonce);

        encryptedKeyBuilder.privateKey(Base64.getEncoder().encodeToString(encryptedPrivateKey))
                .passcodeCheckPlain(passcodeCheckPlain)
                .passcodeCheckEncrypted(Base64.getEncoder().encodeToString(passcodeCheckEncryptedBytes));
    }

    private byte[] remixNonce(final byte[] nonce, final String passcode) {
        byte[] passcodeBytes = passcode.getBytes(StandardCharsets.UTF_8);
        byte[] remixed = Arrays.copyOf(nonce, nonce.length);

        for (int i = 0; i < passcodeBytes.length; i++) {
            int nonceIndex = i % nonce.length;
            remixed[nonceIndex] ^= passcodeBytes[i];
        }

        return remixed;
    }
}
