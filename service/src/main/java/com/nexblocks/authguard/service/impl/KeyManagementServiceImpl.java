package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.config.KeyConfigValue;
import com.nexblocks.authguard.crypto.*;
import com.nexblocks.authguard.crypto.generators.AesParameters;
import com.nexblocks.authguard.crypto.generators.EcSecp256k1Parameters;
import com.nexblocks.authguard.crypto.generators.GeneratorParameters;
import com.nexblocks.authguard.crypto.generators.RsaParameters;
import com.nexblocks.authguard.dal.model.CryptoKeyDO;
import com.nexblocks.authguard.dal.persistence.CryptoKeysRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.KeyManagementService;
import com.nexblocks.authguard.service.config.CryptoKeyConfig;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.model.EphemeralKeyBO;
import com.nexblocks.authguard.service.model.PersistedKeyBO;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class KeyManagementServiceImpl implements KeyManagementService {
    private static final Logger LOG = LoggerFactory.getLogger(ClientsServiceImpl.class);
    private static final String CRYPTO_KEYS_CHANNEL = "crypto_keys";

    private final AccountsService accountsService;
    private final ServiceMapper serviceMapper;
    private final CryptographicRandom cryptographicRandom;
    private final PersistenceService<PersistedKeyBO, CryptoKeyDO, CryptoKeysRepository> persistenceService;
    private final CryptoKeyConfig config;
    private final byte[] encryptionKey;

    @Inject
    public KeyManagementServiceImpl(final AccountsService accountsService,
                                    final ServiceMapper serviceMapper,
                                    final CryptoKeysRepository cryptoKeysRepository,
                                    final MessageBus messageBus,
                                    final @Named("cryptographic_keys") ConfigContext cryptoKeysConfigContext) {
        this.accountsService = accountsService;
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
        return persistenceService.getById(id)
                .thenApply(opt -> opt.map(key -> {
                    byte[] nonce = key.getNonce();
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
        if (Objects.equals(algorithm, "AES")) {
            return generate(Algorithms.aes, new AesParameters(size));
        }

        if (Objects.equals(algorithm, "RSA")) {
            return generate(Algorithms.rsa, new RsaParameters(size));
        }

        if (Objects.equals(algorithm, "EC_SECP128K1")) {
            return generate(Algorithms.ecSecp256k1, new EcSecp256k1Parameters(size));
        }

        throw new ServiceException(ErrorCode.CRYPTO_INVALID_ALGO, "Algorithm " + algorithm +
                " is not supported");
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
        byte[] privateKeyRaw = Base64.getDecoder().decode(key.getPrivateKey());
        byte[] encryptedPrivateKey = ChaCha20Encryptor.encrypt(privateKeyRaw, encryptionKey, nonce);

        return PersistedKeyBO.builder()
                .from(key)
                .privateKey(Base64.getEncoder().encodeToString(encryptedPrivateKey))
                .nonce(nonce)
                .version(config.getVersion())
                .build();
    }
}
