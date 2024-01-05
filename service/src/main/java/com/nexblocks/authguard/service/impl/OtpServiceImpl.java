package com.nexblocks.authguard.service.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.basic.config.OtpConfig;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.OtpService;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.RequestContextBO;

import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<AuthResponseBO> authenticate(final AuthRequestBO authRequest, final RequestContextBO requestContext) {
        return exchangeService.exchange(authRequest, "otp", otpConfig.getGenerateToken(), requestContext);
    }

    @Override
    public CompletableFuture<AuthResponseBO> authenticate(final long passwordId, final String otp, final RequestContextBO requestContext) {
        final String token = passwordId + ":" + otp;

        return authenticate(AuthRequestBO.builder().token(token).build(), requestContext);
    }
}
