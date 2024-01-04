package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.basic.config.OtpConfig;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.dal.model.OneTimePasswordDO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class OtpServiceImplTest {
    private EasyRandom random = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));

    private ExchangeService mockExchangeService;

    private OtpServiceImpl otpService;

    void setup(OtpConfig otpConfig) {
        mockExchangeService = Mockito.mock(ExchangeService.class);

        ConfigContext configContext = Mockito.mock(ConfigContext.class);

        Mockito.when(configContext.asConfigBean(OtpConfig.class)).thenReturn(otpConfig);

        otpService = new OtpServiceImpl(mockExchangeService, configContext);
    }

    @Test
    void authenticate() {
        OtpConfig otpConfig = OtpConfig.builder()
                .generateToken("accessToken")
                .build();

        setup(otpConfig);

        OneTimePasswordDO otp = random.nextObject(OneTimePasswordDO.class);
        AuthResponseBO tokens = random.nextObject(AuthResponseBO.class);

        String otpToken = otp.getId() + ":" + otp.getPassword();
        AuthRequestBO authRequest = AuthRequestBO.builder()
                .token(otpToken)
                .build();
        RequestContextBO requestContext = RequestContextBO.builder().build();

        Mockito.when(mockExchangeService.exchange(authRequest, "otp", otpConfig.getGenerateToken(), requestContext))
                .thenReturn(CompletableFuture.completedFuture(tokens));

        AuthResponseBO generated = otpService.authenticate(otp.getId(), otp.getPassword(), requestContext).join();

        assertThat(generated).isEqualTo(tokens);
    }
}