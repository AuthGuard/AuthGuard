package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.model.TotpKeyDO;
import com.nexblocks.authguard.dal.persistence.TotpKeysRepository;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.config.AuthenticatorConfig;
import com.nexblocks.authguard.service.config.TotpAuthenticatorsConfig;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.TotpKeyBO;
import com.nexblocks.authguard.service.model.UserIdentifier;
import com.nexblocks.authguard.service.model.UserIdentifierBO;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TotpKeysServiceImplTest {
    private static final long ACCOUNT_ID = 1;
    private static final String DOMAIN = "test";
    
    private final AccountBO account = AccountBO.builder()
            .id(ACCOUNT_ID)
            .domain(DOMAIN)
            .addIdentifiers(UserIdentifierBO.builder()
                    .type(UserIdentifier.Type.USERNAME)
                    .identifier("username")
                    .build())
            .build();

    private TotpKeysRepository totpKeysRepository;
    private TotpKeysServiceImpl totpKeysService;
    private AccountsService accountsService;

    @BeforeEach
    void setup() {
        totpKeysRepository = Mockito.mock(TotpKeysRepository.class);
        accountsService = Mockito.mock(AccountsService.class);

        MessageBus messageBus = Mockito.mock(MessageBus.class);
        ConfigContext configContext = Mockito.mock(ConfigContext.class);

        Mockito.when(configContext.asConfigBean(TotpAuthenticatorsConfig.class))
                .thenReturn(TotpAuthenticatorsConfig.builder()
                        .encryptionKey("C2wA8G5AGil0Ofa/agBME69PHKBuPo/xt4AGVCPmOWQ=")
                        .generateQrCode(true)
                        .qrUserIdentifierType(UserIdentifier.Type.USERNAME)
                        .qrIssuer("Tests")
                        .addCustomAuthenticators(AuthenticatorConfig.builder()
                                .name("custom")
                                .timeStep(60)
                                .build())
                        .build());

        Mockito.when(totpKeysRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, TotpKeyDO.class)));

        Mockito.when(accountsService.getById(account.getId(), account.getDomain()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));

        ServiceMapper serviceMapper = new ServiceMapperImpl();
        totpKeysService = new TotpKeysServiceImpl(totpKeysRepository, accountsService, serviceMapper,
                messageBus, configContext);
    }

    @Test
    void generate() {
        Mockito.when(totpKeysRepository.findByAccountId(DOMAIN, ACCOUNT_ID))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        TotpKeyBO generated = totpKeysService.generate(ACCOUNT_ID, DOMAIN, "google")
                .join();

        assertThat(generated.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(generated.getDomain()).isEqualTo(DOMAIN);
        assertThat(generated.getQrCode()).isNotBlank();
        assertThat(generated.getKey()).isNotNull().isNotEmpty();
        assertThat(generated.getAuthenticator()).isEqualTo("google");

        ArgumentCaptor<TotpKeyDO> keyCaptor = ArgumentCaptor.forClass(TotpKeyDO.class);
        Mockito.verify(totpKeysRepository, Mockito.times(1))
                .save(keyCaptor.capture());

        // what is stored is the encrypted one, not the plain one
        assertThat(Arrays.equals(keyCaptor.getValue().getEncryptedKey(), generated.getKey()))
                .isFalse();
    }

    @Test
    void generateWithoutValidIdentifier() {
        AccountBO account = AccountBO.builder()
                .id(2)
                .domain(DOMAIN)
                .addIdentifiers(UserIdentifierBO.builder()
                        .type(UserIdentifier.Type.EMAIL)
                        .identifier("username")
                        .build())
                .build();

        Mockito.when(accountsService.getById(account.getId(), DOMAIN))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));

        Mockito.when(totpKeysRepository.findByAccountId(account.getDomain(), account.getId()))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        CompletableFuture<TotpKeyBO> future = totpKeysService.generate(account.getId(), account.getDomain(), null);

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void getByAccountId() {
        // first generate a key
        Mockito.when(totpKeysRepository.findByAccountId(DOMAIN, ACCOUNT_ID))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        TotpKeyBO generated = totpKeysService.generate(account.getId(), account.getDomain(), "google")
                .join();

        // capture what was stored to get the encrypted value
        ArgumentCaptor<TotpKeyDO> keyCaptor = ArgumentCaptor.forClass(TotpKeyDO.class);
        Mockito.verify(totpKeysRepository, Mockito.times(1))
                .save(keyCaptor.capture());

        Mockito.when(totpKeysRepository.findByAccountId(DOMAIN, ACCOUNT_ID))
                .thenReturn(Uni.createFrom().item(Collections.singletonList(keyCaptor.getValue())));

        // retrieve the key and ensure the encrypted key is what is returned
        List<TotpKeyBO> keys = totpKeysService.getByAccountId(ACCOUNT_ID, DOMAIN).join();

        assertThat(keys).hasSize(1);
        assertThat(keys.get(0)).usingRecursiveComparison()
                .ignoringFields("key", "qrCode")
                .isEqualTo(generated);
        assertThat(keys.get(0).getKey()).containsExactly(keyCaptor.getValue().getEncryptedKey());
    }

    @Test
    void getByAccountIdDecrypted() {
        // first generate a key
        Mockito.when(totpKeysRepository.findByAccountId(DOMAIN, ACCOUNT_ID))
                .thenReturn(Uni.createFrom().item(Collections.emptyList()));

        TotpKeyBO generated = totpKeysService.generate(ACCOUNT_ID, DOMAIN, null)
                .join();

        // capture what was stored to get the encrypted value
        ArgumentCaptor<TotpKeyDO> keyCaptor = ArgumentCaptor.forClass(TotpKeyDO.class);
        Mockito.verify(totpKeysRepository, Mockito.times(1))
                .save(keyCaptor.capture());

        Mockito.when(totpKeysRepository.findByAccountId(DOMAIN, ACCOUNT_ID))
                .thenReturn(Uni.createFrom().item(Collections.singletonList(keyCaptor.getValue())));

        // retrieve the key and ensure the encrypted key is what is returned
        Optional<TotpKeyBO> keys = totpKeysService.getByAccountIdDecrypted(ACCOUNT_ID, DOMAIN).join();

        assertThat(keys).isNotEmpty();
        assertThat(keys.get()).usingRecursiveComparison()
                .ignoringFields("key", "qrCode")
                .isEqualTo(generated);
        assertThat(keys.get().getKey()).containsExactly(generated.getKey());
    }
}