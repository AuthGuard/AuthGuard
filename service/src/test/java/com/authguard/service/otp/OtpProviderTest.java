package com.authguard.service.otp;

import com.authguard.config.ConfigContext;
import com.authguard.dal.OtpRepository;
import com.authguard.dal.model.OneTimePasswordDO;
import com.authguard.service.config.OtpConfig;
import com.authguard.service.config.OtpMode;
import com.authguard.service.mappers.ServiceMapperImpl;
import com.authguard.service.model.AccountBO;
import com.authguard.service.model.TokensBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OtpProviderTest {
    private final EasyRandom random = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

    private OtpRepository mockOtpRepository;

    private OtpProvider otpProvider;

    void setup(final OtpConfig otpConfig) {
        mockOtpRepository = Mockito.mock(OtpRepository.class);

        final ConfigContext configContext = Mockito.mock(ConfigContext.class);

        Mockito.when(configContext.asConfigBean(OtpConfig.class)).thenReturn(otpConfig);

        otpProvider = new OtpProvider(mockOtpRepository, new ServiceMapperImpl(), configContext);
    }

    @Test
    void generateToken() {
        final OtpConfig otpConfig = OtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        final AccountBO account = random.nextObject(AccountBO.class);

        final TokensBO generated = otpProvider.generateToken(account);

        assertThat(generated.getType()).isEqualTo("OTP");
        assertThat(generated.getToken()).isNotNull();
        assertThat(generated.getRefreshToken()).isNull();

        final ArgumentCaptor<OneTimePasswordDO> argumentCaptor = ArgumentCaptor.forClass(OneTimePasswordDO.class);

        Mockito.verify(mockOtpRepository).save(argumentCaptor.capture());

        final OneTimePasswordDO persisted = argumentCaptor.getValue();

        assertThat(persisted.getAccountId()).isEqualTo(account.getId());
        assertThat(persisted.getExpiresAt())
                .isAfter(ZonedDateTime.now())
                .isBefore(ZonedDateTime.now().plus(Duration.ofMinutes(6)));
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getPassword()).isNotNull();
        assertThat(persisted.getPassword()).hasSize(6);
    }

    @Test
    void generateAlphabetic() {
        final OtpConfig otpConfig = OtpConfig.builder()
                .mode(OtpMode.ALPHABETIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        final AccountBO account = random.nextObject(AccountBO.class);

        final TokensBO generated = otpProvider.generateToken(account);

        assertThat(generated.getType()).isEqualTo("OTP");
        assertThat(generated.getToken()).isNotNull();
        assertThat(generated.getRefreshToken()).isNull();

        final ArgumentCaptor<OneTimePasswordDO> argumentCaptor = ArgumentCaptor.forClass(OneTimePasswordDO.class);

        Mockito.verify(mockOtpRepository).save(argumentCaptor.capture());

        final OneTimePasswordDO persisted = argumentCaptor.getValue();

        assertThat(persisted.getAccountId()).isEqualTo(account.getId());
        assertThat(persisted.getExpiresAt())
                .isAfter(ZonedDateTime.now())
                .isBefore(ZonedDateTime.now().plus(Duration.ofMinutes(6)));
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getPassword()).isNotNull();
        assertThat(persisted.getPassword()).hasSize(6);

        for (final char ch : persisted.getPassword().toCharArray()) {
            assertThat(Character.isDigit(ch)).isFalse();
        }
    }

    @Test
    void generateNumeric() {
        final OtpConfig otpConfig = OtpConfig.builder()
                .mode(OtpMode.NUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        final AccountBO account = random.nextObject(AccountBO.class);

        final TokensBO generated = otpProvider.generateToken(account);

        assertThat(generated.getType()).isEqualTo("OTP");
        assertThat(generated.getToken()).isNotNull();
        assertThat(generated.getRefreshToken()).isNull();

        final ArgumentCaptor<OneTimePasswordDO> argumentCaptor = ArgumentCaptor.forClass(OneTimePasswordDO.class);

        Mockito.verify(mockOtpRepository).save(argumentCaptor.capture());

        final OneTimePasswordDO persisted = argumentCaptor.getValue();

        assertThat(persisted.getAccountId()).isEqualTo(account.getId());
        assertThat(persisted.getExpiresAt())
                .isAfter(ZonedDateTime.now())
                .isBefore(ZonedDateTime.now().plus(Duration.ofMinutes(6)));
        assertThat(persisted.getId()).isNotNull();
        assertThat(persisted.getPassword()).isNotNull();
        assertThat(persisted.getPassword()).hasSize(6);

        for (final char ch : persisted.getPassword().toCharArray()) {
            assertThat(Character.isAlphabetic(ch)).isFalse();
        }
    }
}