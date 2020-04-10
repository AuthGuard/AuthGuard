package com.authguard.service.otp;

import com.authguard.config.ConfigContext;
import com.authguard.dal.OtpRepository;
import com.authguard.dal.model.OneTimePasswordDO;
import com.authguard.service.config.OtpConfig;
import com.authguard.service.config.OtpMode;
import com.authguard.service.exceptions.ServiceAuthorizationException;
import com.authguard.service.mappers.ServiceMapperImpl;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OtpVerifierTest {

    private final EasyRandom random = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

    private OtpRepository mockOtpRepository;

    private OtpVerifier otpVerifier;

    void setup(final OtpConfig otpConfig) {
        mockOtpRepository = Mockito.mock(OtpRepository.class);

        final ConfigContext configContext = Mockito.mock(ConfigContext.class);

        Mockito.when(configContext.asConfigBean(OtpConfig.class)).thenReturn(otpConfig);

        otpVerifier = new OtpVerifier(mockOtpRepository, new ServiceMapperImpl());
    }

    @Test
    void verify() {
        final OtpConfig otpConfig = OtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        final OneTimePasswordDO otp = random.nextObject(OneTimePasswordDO.class);

        Mockito.when(mockOtpRepository.getById(otp.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(otp)));

        final Optional<String> generated = otpVerifier.verifyAccountToken(otp.getId() + ":" + otp.getPassword());

        assertThat(generated).contains(otp.getAccountId());
    }

    @Test
    void verifyWrongPassword() {
        final OtpConfig otpConfig = OtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        final OneTimePasswordDO otp = random.nextObject(OneTimePasswordDO.class);

        Mockito.when(mockOtpRepository.getById(otp.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(otp)));

        assertThat(otpVerifier.verifyAccountToken(otp.getId() + ":" + "wrong")).isEmpty();
    }

    @Test
    void verifyInvalidOtpFormat() {
        final OtpConfig otpConfig = OtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        assertThatThrownBy(() -> otpVerifier.verifyAccountToken("not a valid OTP"))
                .isInstanceOf(ServiceAuthorizationException.class);
    }

    @Test
    void verifyPasswordNotFound() {
        final OtpConfig otpConfig = OtpConfig.builder()
                .mode(OtpMode.ALPHANUMERIC)
                .length(6)
                .lifeTime("5m")
                .build();

        setup(otpConfig);

        final OneTimePasswordDO otp = random.nextObject(OneTimePasswordDO.class);

        Mockito.when(mockOtpRepository.getById(otp.getId()))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        assertThatThrownBy(() -> otpVerifier.verifyAccountToken(otp.getId() + ":" + otp.getPassword()))
                .isInstanceOf(ServiceAuthorizationException.class);
    }
}