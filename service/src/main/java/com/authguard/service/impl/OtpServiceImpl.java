package com.authguard.service.impl;

import com.authguard.config.ConfigContext;
import com.authguard.service.ExchangeService;
import com.authguard.service.config.OtpConfig;
import com.authguard.service.model.AuthRequestBO;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.authguard.service.OtpService;
import com.authguard.service.model.TokensBO;

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
