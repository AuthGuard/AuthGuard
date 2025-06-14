package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.crypto.GeneratorResult;
import com.nexblocks.authguard.crypto.generators.EcSecp256k1Generator;
import com.nexblocks.authguard.crypto.generators.EcSecp256k1Parameters;
import com.nexblocks.authguard.dal.model.CryptoKeyDO;
import com.nexblocks.authguard.dal.persistence.CryptoKeysRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.ApplicationsService;
import com.nexblocks.authguard.service.config.CryptoKeyConfig;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.PersistedKeyBO;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyManagementServiceImplTest {
    private CryptoKeysRepository cryptoKeysRepository;
    private AccountsService accountsService;
    private ApplicationsService applicationsService;
    private KeyManagementServiceImpl keyManagementService;

    @BeforeEach
    void setup() {
        cryptoKeysRepository = Mockito.mock(CryptoKeysRepository.class);
        accountsService = Mockito.mock(AccountsService.class);
        applicationsService = Mockito.mock(ApplicationsService.class);

        MessageBus messageBus = Mockito.mock(MessageBus.class);
        ConfigContext configContext = Mockito.mock(ConfigContext.class);

        CryptoKeyConfig cryptoKeyConfig = CryptoKeyConfig.builder()
                .encryptionKey("C2wA8G5AGil0Ofa/agBME69PHKBuPo/xt4AGVCPmOWQ=")
                .build();

        Mockito.when(configContext.asConfigBean(CryptoKeyConfig.class))
                .thenReturn(cryptoKeyConfig);

        ServiceMapper serviceMapper = new ServiceMapperImpl();
        keyManagementService = new KeyManagementServiceImpl(accountsService, applicationsService, serviceMapper,
                cryptoKeysRepository,
                messageBus, configContext);
    }

    @Test
    void createAndGetWithoutPasscode() {
        EcSecp256k1Generator generator = new EcSecp256k1Generator();
        GeneratorResult generatedKeys = generator.generate(new EcSecp256k1Parameters(256));

        PersistedKeyBO key = PersistedKeyBO.builder()
                .domain("main")
                .algorithm("EC")
                .privateKey(Base64.getEncoder().encodeToString(generatedKeys.getPrivateKey()))
                .publicKey(Base64.getEncoder().encodeToString(generatedKeys.getPublicKey()))
                .build();

        Mockito.when(cryptoKeysRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, CryptoKeyDO.class)));

        PersistedKeyBO persisted = keyManagementService.create(key).subscribeAsCompletionStage().join();

        assertThat(Arrays.equals(Base64.getDecoder().decode(persisted.getPrivateKey()), generatedKeys.getPrivateKey()))
                .isTrue();

        ArgumentCaptor<CryptoKeyDO> keyCaptor = ArgumentCaptor.forClass(CryptoKeyDO.class);
        Mockito.verify(cryptoKeysRepository, Mockito.times(1))
                .save(keyCaptor.capture());

        assertThat(keyCaptor.getValue().isPasscodeProtected()).isFalse();
        assertThat(Arrays.equals(keyCaptor.getValue().getPrivateKey(), generatedKeys.getPrivateKey()))
                .isFalse();

        // get decrypted
        Mockito.when(cryptoKeysRepository.getById(Mockito.anyLong()))
                .thenReturn(Uni.createFrom().item(Optional.of(keyCaptor.getValue())));

        Optional<PersistedKeyBO> retrieved = keyManagementService.getDecrypted(1, "main", null).subscribeAsCompletionStage().join();

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getPrivateKey())
                .isEqualTo(Base64.getEncoder().encodeToString(generatedKeys.getPrivateKey()));
    }

    @Test
    void createAndGetWithPasscode() {
        EcSecp256k1Generator generator = new EcSecp256k1Generator();
        GeneratorResult generatedKeys = generator.generate(new EcSecp256k1Parameters(256));

        PersistedKeyBO key = PersistedKeyBO.builder()
                .domain("main")
                .algorithm("EC")
                .privateKey(Base64.getEncoder().encodeToString(generatedKeys.getPrivateKey()))
                .publicKey(Base64.getEncoder().encodeToString(generatedKeys.getPublicKey()))
                .passcodeProtected(true)
                .passcode("pass")
                .build();

        Mockito.when(cryptoKeysRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, CryptoKeyDO.class)));

        PersistedKeyBO persisted = keyManagementService.create(key).subscribeAsCompletionStage().join();

        assertThat(Arrays.equals(Base64.getDecoder().decode(persisted.getPrivateKey()), generatedKeys.getPrivateKey()))
                .isTrue();

        ArgumentCaptor<CryptoKeyDO> keyCaptor = ArgumentCaptor.forClass(CryptoKeyDO.class);
        Mockito.verify(cryptoKeysRepository, Mockito.times(1))
                .save(keyCaptor.capture());

        assertThat(keyCaptor.getValue().isPasscodeProtected()).isTrue();
        assertThat(Arrays.equals(keyCaptor.getValue().getPrivateKey(), generatedKeys.getPrivateKey()))
                .isFalse();

        // get decrypted
        Mockito.when(cryptoKeysRepository.getById(Mockito.anyLong()))
                .thenReturn(Uni.createFrom().item(Optional.of(keyCaptor.getValue())));

        Optional<PersistedKeyBO> retrieved = keyManagementService.getDecrypted(1, "main", "pass").subscribeAsCompletionStage().join();

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getPrivateKey())
                .isEqualTo(Base64.getEncoder().encodeToString(generatedKeys.getPrivateKey()));
    }

    @Test
    void createAndGetWithTheWrongPasscode() {
        EcSecp256k1Generator generator = new EcSecp256k1Generator();
        GeneratorResult generatedKeys = generator.generate(new EcSecp256k1Parameters(256));

        PersistedKeyBO key = PersistedKeyBO.builder()
                .domain("main")
                .algorithm("EC")
                .privateKey(Base64.getEncoder().encodeToString(generatedKeys.getPrivateKey()))
                .publicKey(Base64.getEncoder().encodeToString(generatedKeys.getPublicKey()))
                .passcodeProtected(true)
                .passcode("pass")
                .build();

        Mockito.when(cryptoKeysRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, CryptoKeyDO.class)));

        keyManagementService.create(key).subscribeAsCompletionStage().join();

        ArgumentCaptor<CryptoKeyDO> keyCaptor = ArgumentCaptor.forClass(CryptoKeyDO.class);
        Mockito.verify(cryptoKeysRepository, Mockito.times(1))
                .save(keyCaptor.capture());

        // get decrypted
        Mockito.when(cryptoKeysRepository.getById(Mockito.anyLong()))
                .thenReturn(Uni.createFrom().item(Optional.of(keyCaptor.getValue())));

        assertThatThrownBy(() -> keyManagementService.getDecrypted(1, "main", "pas").subscribeAsCompletionStage().join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void createWithAccountId() {
        EcSecp256k1Generator generator = new EcSecp256k1Generator();
        GeneratorResult generatedKeys = generator.generate(new EcSecp256k1Parameters(256));

        PersistedKeyBO key = PersistedKeyBO.builder()
                .domain("main")
                .algorithm("EC")
                .accountId(100L)
                .privateKey(Base64.getEncoder().encodeToString(generatedKeys.getPrivateKey()))
                .publicKey(Base64.getEncoder().encodeToString(generatedKeys.getPublicKey()))
                .build();

        Mockito.when(cryptoKeysRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, CryptoKeyDO.class)));

        Mockito.when(accountsService.getById(100L, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(AccountBO.builder().build())));

        PersistedKeyBO persisted = keyManagementService.create(key).subscribeAsCompletionStage().join();

        assertThat(persisted.getAccountId()).isEqualTo(key.getAccountId());
    }

    @Test
    void createWithAppId() {
        EcSecp256k1Generator generator = new EcSecp256k1Generator();
        GeneratorResult generatedKeys = generator.generate(new EcSecp256k1Parameters(256));

        PersistedKeyBO key = PersistedKeyBO.builder()
                .domain("main")
                .algorithm("EC")
                .appId(100L)
                .privateKey(Base64.getEncoder().encodeToString(generatedKeys.getPrivateKey()))
                .publicKey(Base64.getEncoder().encodeToString(generatedKeys.getPublicKey()))
                .build();

        Mockito.when(cryptoKeysRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, CryptoKeyDO.class)));

        Mockito.when(applicationsService.getById(100L, "main"))
                .thenReturn(Uni.createFrom().item(Optional.of(AppBO.builder().build())));

        PersistedKeyBO persisted = keyManagementService.create(key).subscribeAsCompletionStage().join();

        assertThat(persisted.getAppId()).isEqualTo(key.getAppId());
    }

    @Test
    void createWithInvalidAccountId() {
        EcSecp256k1Generator generator = new EcSecp256k1Generator();
        GeneratorResult generatedKeys = generator.generate(new EcSecp256k1Parameters(256));

        PersistedKeyBO key = PersistedKeyBO.builder()
                .domain("main")
                .algorithm("EC")
                .accountId(100L)
                .privateKey(Base64.getEncoder().encodeToString(generatedKeys.getPrivateKey()))
                .publicKey(Base64.getEncoder().encodeToString(generatedKeys.getPublicKey()))
                .build();

        Mockito.when(cryptoKeysRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, CryptoKeyDO.class)));

        Mockito.when(accountsService.getById(100L, "main"))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        assertThatThrownBy(() -> keyManagementService.create(key).subscribeAsCompletionStage().join())
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void createWithInvalidAppId() {
        EcSecp256k1Generator generator = new EcSecp256k1Generator();
        GeneratorResult generatedKeys = generator.generate(new EcSecp256k1Parameters(256));

        PersistedKeyBO key = PersistedKeyBO.builder()
                .domain("main")
                .algorithm("EC")
                .appId(100L)
                .privateKey(Base64.getEncoder().encodeToString(generatedKeys.getPrivateKey()))
                .publicKey(Base64.getEncoder().encodeToString(generatedKeys.getPublicKey()))
                .build();

        Mockito.when(cryptoKeysRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, CryptoKeyDO.class)));

        Mockito.when(applicationsService.getById(100L, "main"))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        assertThatThrownBy(() -> keyManagementService.create(key).subscribeAsCompletionStage().join())
                .hasCauseInstanceOf(ServiceException.class);
    }
}