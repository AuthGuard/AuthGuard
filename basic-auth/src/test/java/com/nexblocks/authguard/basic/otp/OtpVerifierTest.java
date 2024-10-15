package com.nexblocks.authguard.basic.otp;

import com.nexblocks.authguard.basic.config.OtpConfig;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.OtpRepository;
import com.nexblocks.authguard.dal.model.OneTimePasswordDO;
import com.nexblocks.authguard.basic.config.OtpMode;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtpVerifierTest {

    private final EasyRandom random = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

    private OtpRepository mockOtpRepository;

    private OtpVerifier otpVerifier;

    void setup(OtpConfig otpConfig) {
        mockOtpRepository = Mockito.mock(OtpRepository.class);

        ConfigContext configContext = Mockito.mock(ConfigContext.class);

        Mockito.when(configContext.asConfigBean(OtpConfig.class)).thenReturn(otpConfig);

        otpVerifier = new OtpVerifier(mockOtpRepository, new ServiceMapperImpl());
    }

    @Test
    void verify() {
        OtpConfig otpConfig = OtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        OneTimePasswordDO otp = OneTimePasswordDO.builder()
                .id(123)
                .password("abcde")
                .expiresAt(Instant.now().plusSeconds(1))
                .build();

        Mockito.when(mockOtpRepository.getById(otp.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(otp)));

        Long generated = otpVerifier.verifyAccountTokenAsync(otp.getId() + ":" + otp.getPassword()).join();

        assertThat(generated).isEqualTo(otp.getAccountId());
    }

    @Test
    void verifyWrongPassword() {
        OtpConfig otpConfig = OtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        OneTimePasswordDO otp = random.nextObject(OneTimePasswordDO.class);

        Mockito.when(mockOtpRepository.getById(otp.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(otp)));

        assertThatThrownBy(() -> otpVerifier.verifyAccountTokenAsync(otp.getId() + ":" + "wrong").join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void verifyInvalidOtpFormat() {
        OtpConfig otpConfig = OtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        assertThatThrownBy(() -> otpVerifier.verifyAccountTokenAsync("not a valid OTP").join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void verifyPasswordNotFound() {
        OtpConfig otpConfig = OtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        OneTimePasswordDO otp = random.nextObject(OneTimePasswordDO.class);

        Mockito.when(mockOtpRepository.getById(otp.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> otpVerifier.verifyAccountTokenAsync("not a valid OTP").join())
                .hasCauseInstanceOf(ServiceAuthorizationException.class);
    }
}