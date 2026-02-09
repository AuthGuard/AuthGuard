package com.nexblocks.authguard.saml.exchange;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.saml.SamlAuthnRequest;
import com.nexblocks.authguard.saml.SamlConditionalAuthn;
import com.nexblocks.authguard.saml.SamlErrorResponseProvider;
import com.nexblocks.authguard.saml.config.ImmutableSamlConfiguration;
import com.nexblocks.authguard.saml.config.SamlConfiguration;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.exchange.Exchange;
import com.nexblocks.authguard.service.exchange.TokenExchange;
import com.nexblocks.authguard.service.mappers.TokenOptionsMapper;
import com.nexblocks.authguard.service.model.*;
import io.smallrye.mutiny.Uni;
import org.opensaml.saml.saml2.core.Response;

import java.time.Instant;
import java.util.Objects;

@TokenExchange(from = "ssoSession", to = "samlResponse")
public class SsoSessionToSamlResponse implements Exchange {
    private final AccountTokensRepository accountTokensRepository;
    private final SamlResponseProvider samlResponseProvider;
    private final AccountsService accountsService;
    private final SamlConfiguration configuration;

    @Inject
    public SsoSessionToSamlResponse(final AccountTokensRepository accountTokensRepository,
                                    final AccountsService accountsService,
                                    final @Named("saml") ConfigContext configContext) {
        this(accountTokensRepository,
                accountsService,
                configContext.asConfigBean(ImmutableSamlConfiguration.class));
    }

    public SsoSessionToSamlResponse(final AccountTokensRepository accountTokensRepository,
                                    final AccountsService accountsService,
                                    final SamlConfiguration configuration) {
        this.accountTokensRepository = accountTokensRepository;
        this.samlResponseProvider = new SamlResponseProvider(configuration);
        this.accountsService = accountsService;
        this.configuration = configuration;
    }

    @Override
    public Uni<AuthResponseBO> exchange(final AuthRequestBO request) {
        SamlAuthnRequest authnRequest = (SamlAuthnRequest) request.getExtraParameters();

        return accountTokensRepository.getByToken(request.getToken())
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new ServiceException(ErrorCode.INVALID_TOKEN, ""));
                    }

                    return verifySession(opt.get(), request);
                })
                .flatMap(session -> accountsService.getById(session.getAssociatedAccountId(), request.getDomain())
                        .flatMap(accountOpt -> {
                            if (accountOpt.isEmpty()) {
                                return Uni.createFrom().failure(new ServiceException(ErrorCode.ACCOUNT_DOES_NOT_EXIST, ""));
                            }

                            return processRequest(authnRequest, request, session, accountOpt.get());
                        }));
    }

    private Uni<AccountTokenDO> verifySession(final AccountTokenDO session,
                                              final AuthRequest request) {
        if (Objects.equals(session.getDomain(), request.getDomain())
                || session.getExpiresAt().isBefore(Instant.now())) {

            return Uni.createFrom().failure(new ServiceException(ErrorCode.EXPIRED_TOKEN, ""));
        }

        return Uni.createFrom().item(session);
    }

    private Uni<AuthResponseBO> processRequest(final SamlAuthnRequest authnRequest,
                                               final AuthRequestBO request,
                                               final AccountTokenDO session,
                                               final AccountBO account) {
        if (!SamlConditionalAuthn.satisfiesRequestedContext(authnRequest, session.getSourceAuthType())) {
            Response error = SamlErrorResponseProvider.authnFailed(
                    configuration.getIssuer(),
                    authnRequest.getAcsUrl(),
                    authnRequest.getRequestId(),
                    "RequestedAuthnContext not satisfied");

            AuthResponseBO response = samlResponseProvider.generateError(authnRequest, error);

            return Uni.createFrom().item(response);
        }

        TokenOptionsBO options = TokenOptionsMapper.fromAuthRequest(request)
                .source(session.getSourceAuthType())
                .trackingSession(request.getTrackingSession())
                .extraParameters(request.getExtraParameters())
                .build();

        return Uni.createFrom().item(samlResponseProvider.generateToken(authnRequest, account, options));
    }
}
