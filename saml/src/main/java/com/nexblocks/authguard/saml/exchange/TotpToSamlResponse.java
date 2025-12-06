package com.nexblocks.authguard.saml.exchange;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.basic.totp.TotpVerifier;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.saml.SamlAuthnRequest;
import com.nexblocks.authguard.saml.SamlConditionalAuthn;
import com.nexblocks.authguard.saml.SamlErrorResponseProvider;
import com.nexblocks.authguard.saml.config.ImmutableSamlConfiguration;
import com.nexblocks.authguard.saml.config.SamlConfiguration;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.mappers.TokenOptionsMapper;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;
import io.smallrye.mutiny.Uni;
import org.opensaml.saml.saml2.core.Response;

@TokenExchange(from = "totp", to = "samlResponse")
public class TotpToSamlResponse implements Exchange {
    private final TotpVerifier totpVerifier;
    private final AccountsService accountsService;
    private final SamlResponseProvider samlResponseProvider;
    private final SamlConfiguration configuration;

    @Inject
    public TotpToSamlResponse(final TotpVerifier otpVerifier, final AccountsService accountsService,
                              final @Named("saml") ConfigContext samlConfig) {
        this(otpVerifier, accountsService, samlConfig.asConfigBean(ImmutableSamlConfiguration.class));
    }

    public TotpToSamlResponse(final TotpVerifier totpProvider, final AccountsService accountsService,
                              final SamlConfiguration samlConfig) {
        this.accountsService = accountsService;
        this.totpVerifier = totpProvider;
        this.samlResponseProvider = new SamlResponseProvider(samlConfig);
        this.configuration = samlConfig;
    }

    @Override
    public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
        if (request.getExtraParameters() == null || !SamlAuthnRequest.class.isAssignableFrom(request.getExtraParameters().getClass())) {
            return Uni.createFrom().failure(new ServiceException(ErrorCode.INVALID_TOKEN, "Missing AuthnRequest parameters"));
        }

        SamlAuthnRequest authnRequest = (SamlAuthnRequest) request.getExtraParameters();

        if (!SamlConditionalAuthn.satisfiesRequestedContext(authnRequest, "totp")) {
            Response error = SamlErrorResponseProvider.authnFailed(
                    configuration.getIssuer(),
                    authnRequest.getAcsUrl(),
                    authnRequest.getRequestId(),
                    "RequestedAuthnContext not satisfied");

            AuthResponseBO response = samlResponseProvider.generateError(authnRequest, error);

            return Uni.createFrom().item(response);
        }

        return totpVerifier.verifyAccountTokenAsync(request)
                .flatMap(id -> accountsService.getById(id, request.getDomain()))
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.ACCOUNT_DOES_NOT_EXIST,
                                "The account associated with that token does not exist"));
                    }

                    TokenOptionsBO options = TokenOptionsMapper.fromAuthRequest(request)
                            .source("totp")
                            .trackingSession(request.getTrackingSession())
                            .extraParameters(request.getExtraParameters())
                            .build();

                    AuthResponseBO response = samlResponseProvider.generateToken(authnRequest,
                            opt.get(),
                            options);

                    return Uni.createFrom().item(response);
                });
    }
}
