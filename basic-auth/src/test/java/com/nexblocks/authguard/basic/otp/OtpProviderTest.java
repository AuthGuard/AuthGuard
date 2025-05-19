package com.nexblocks.authguard.basic.otp;

import com.nexblocks.authguard.basic.config.OtpConfig;
import com.nexblocks.authguard.basic.config.OtpMode;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.OtpRepository;
import com.nexblocks.authguard.dal.model.OneTimePasswordDO;
import com.nexblocks.authguard.emb.MessageBus;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.EntityType;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import io.smallrye.mutiny.Uni;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class OtpProviderTest {
    private EasyRandom random = new EasyRandom(new EasyRandomParameters()
            .excludeField(field -> field.getName().equals("initShim"))
            .collectionSizeRange(1, 4));

    private OtpRepository mockOtpRepository;
    private MessageBus messageBus;

    private OtpProvider otpProvider;

    void setup(final OtpConfig otpConfig) {
        mockOtpRepository = Mockito.mock(OtpRepository.class);
        messageBus = Mockito.mock(MessageBus.class);

        ConfigContext configContext = Mockito.mock(ConfigContext.class);

        Mockito.when(configContext.asConfigBean(OtpConfig.class)).thenReturn(otpConfig);
        Mockito.when(mockOtpRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, OneTimePasswordDO.class)));

        otpProvider = new OtpProvider(mockOtpRepository, new ServiceMapperImpl(), messageBus, configContext);
    }

    @Test
    void generateToken() {
        OtpConfig otpConfig = OtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        AccountBO account = random.nextObject(AccountBO.class).withActive(true);

        AuthResponseBO expected = AuthResponseBO.builder()
                .type("otp")
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .build();

        TokenOptionsBO tokenOptions = TokenOptionsBO.builder().build();

        AuthResponseBO generated = otpProvider.generateToken(account, tokenOptions).join();

        assertThat(generated)
                .usingRecursiveComparison()
                .ignoringFields("token")
                .isEqualTo(expected);
        assertThat(generated.getToken()).isNotNull();

        ArgumentCaptor<OneTimePasswordDO> argumentCaptor = ArgumentCaptor.forClass(OneTimePasswordDO.class);

        Mockito.verify(mockOtpRepository).save(argumentCaptor.capture());

        OneTimePasswordDO persisted = argumentCaptor.getValue();

        assertThat(persisted.getAccountId()).isEqualTo(account.getId());
        assertThat(persisted.getExpiresAt())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(Duration.ofMinutes(6)));
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getPassword()).isNotNull();
        assertThat(persisted.getPassword()).hasSize(6);

        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("otp"), any());
    }

    @Test
    void generateAlphabetic() {
        OtpConfig otpConfig = OtpConfig.builder()
                .mode(OtpMode.ALPHABETIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        AccountBO account = random.nextObject(AccountBO.class).withActive(true);

        AuthResponseBO expected = AuthResponseBO.builder()
                .type("otp")
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .build();

        TokenOptionsBO tokenOptions = TokenOptionsBO.builder().build();

        AuthResponseBO generated = otpProvider.generateToken(account, tokenOptions).join();

        assertThat(generated)
                .usingRecursiveComparison()
                .ignoringFields("token")
                .isEqualTo(expected);
        assertThat(generated.getToken()).isNotNull();

        ArgumentCaptor<OneTimePasswordDO> argumentCaptor = ArgumentCaptor.forClass(OneTimePasswordDO.class);

        Mockito.verify(mockOtpRepository).save(argumentCaptor.capture());

        OneTimePasswordDO persisted = argumentCaptor.getValue();

        assertThat(persisted.getAccountId()).isEqualTo(account.getId());
        assertThat(persisted.getExpiresAt())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(Duration.ofMinutes(6)));
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getPassword()).isNotNull();
        assertThat(persisted.getPassword()).hasSize(6);

        for (final char ch : persisted.getPassword().toCharArray()) {
            assertThat(Character.isDigit(ch)).isFalse();
        }

        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("otp"), any());
    }

    @Test
    void generateNumeric() {
        OtpConfig otpConfig = OtpConfig.builder()
                .mode(OtpMode.NUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        AccountBO account = random.nextObject(AccountBO.class).withActive(true);

        TokenOptionsBO tokenOptions = TokenOptionsBO.builder().build();

        AuthResponseBO expected = AuthResponseBO.builder()
                .type("otp")
                .entityType(EntityType.ACCOUNT)
                .entityId(account.getId())
                .build();

        AuthResponseBO generated = otpProvider.generateToken(account, tokenOptions).join();

        assertThat(generated)
                .usingRecursiveComparison()
                .ignoringFields("token")
                .isEqualTo(expected);
        assertThat(generated.getToken()).isNotNull();

        ArgumentCaptor<OneTimePasswordDO> argumentCaptor = ArgumentCaptor.forClass(OneTimePasswordDO.class);

        Mockito.verify(mockOtpRepository).save(argumentCaptor.capture());

        OneTimePasswordDO persisted = argumentCaptor.getValue();

        assertThat(persisted.getAccountId()).isEqualTo(account.getId());
        assertThat(persisted.getExpiresAt())
                .isAfter(Instant.now())
                .isBefore(Instant.now().plus(Duration.ofMinutes(6)));
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getPassword()).isNotNull();
        assertThat(persisted.getPassword()).hasSize(6);

        for (final char ch : persisted.getPassword().toCharArray()) {
            assertThat(Character.isAlphabetic(ch)).isFalse();
        }

        Mockito.verify(messageBus, Mockito.times(1))
                .publish(eq("otp"), any());
    }

    @Test
    void generateTokenForInactiveAccount() {
        OtpConfig otpConfig = OtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        AccountBO account = random.nextObject(AccountBO.class).withActive(false);
        TokenOptionsBO tokenOptions = TokenOptionsBO.builder().build();

        assertThatThrownBy(() -> otpProvider.generateToken(account, tokenOptions))
                .isInstanceOf(ServiceAuthorizationException.class);
    }
}