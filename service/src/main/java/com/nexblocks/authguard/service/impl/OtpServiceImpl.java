package com.nexblocks.authguard.service.impl;

import com.nexblocks.authguard.basic.config.OtpConfig;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.OtpService;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class OtpServiceImpl implements OtpService {
    private final ExchangeService exchangeService;
    private final OtpConfig otpConfig;

    @Inject
    public OtpServiceImpl(final ExchangeService exchangeService,
                          @Named("otp") final ConfigContext configContext) {
        this.exchangeService = exchangeService;
        this.otpConfig = configContext.asConfigBean(OtpConfig.class);
    }

    @Override
    public TokensBO authenticate(final AuthRequestBO authRequest) {
        return exchangeService.exchange(authRequest, "otp", otpConfig.getGenerateToken());
    }

    @Override
    public TokensBO authenticate(final String passwordId, final String otp) {
        final String token = passwordId + ":" + otp;

        return authenticate(AuthRequestBO.builder().token(token).build());
    }
}
