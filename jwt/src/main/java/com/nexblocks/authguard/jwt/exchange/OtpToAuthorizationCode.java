package com.nexblocks.authguard.jwt.exchange;

import com.google.inject.Inject;
import com.nexblocks.authguard.basic.otp.OtpVerifier;
import com.nexblocks.authguard.jwt.oauth.AuthorizationCodeProvider;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.mappers.TokenOptionsMapper;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import io.smallrye.mutiny.Uni;

@TokenExchange(from = "otp", to = "authorizationCode")
public class OtpToAuthorizationCode implements Exchange {
    private final OtpVerifier otpVerifier;
    private final AuthorizationCodeProvider authorizationCodeProvider;
    private final AccountsService accountsService;

    @Inject
    public OtpToAuthorizationCode(final OtpVerifier basicAuth,
                                  final AuthorizationCodeProvider authorizationCodeProvider,
                                  final AccountsService accountsService) {
        this.otpVerifier = basicAuth;
        this.authorizationCodeProvider = authorizationCodeProvider;
        this.accountsService = accountsService;
    }

    @Override
    public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
        return otpVerifier.verifyAccountTokenAsync(request)
                .flatMap(id -> accountsService.getById(id, request.getDomain()))
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                                "The account associated with that token does not exist"));
                    }

                    TokenOptionsBO options = TokenOptionsMapper.fromAuthRequest(request)
                            .source("otp")
                            .trackingSession(request.getTrackingSession())
                            .extraParameters(request.getExtraParameters())
                            .build();

                    return authorizationCodeProvider.generateToken(opt.get(),
                            request.getRestrictions(), options);
                });
    }
}
