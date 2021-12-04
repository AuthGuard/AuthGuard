package com.nexblocks.authguard.jwt.oauth.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.jwt.oauth.OAuthServiceClient;
import com.nexblocks.authguard.jwt.oauth.ResponseType;
import com.nexblocks.authguard.jwt.oauth.TokensResponse;
import com.nexblocks.authguard.jwt.oauth.config.ImmutableOAuthClientConfiguration;
import com.nexblocks.authguard.jwt.oauth.config.ImmutableOAuthConfiguration;
import com.nexblocks.authguard.service.AccountsService;
import com.nexblocks.authguard.service.SessionsService;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AccountEmailBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.model.SessionBO;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class OAuthService {
    private static final Logger LOG = LoggerFactory.getLogger(OAuthService.class);

    private final Map<String, OAuthServiceClient> providersClients;
    private final SessionsService sessionsService;
    private final AccountsService accountsService;
    private final Duration stateTtl;

    @Inject
    public OAuthService(final @Named("oauth") ConfigContext configContext,
                        final SessionsService sessionsService,
                        final AccountsService accountsService) {
        this(configContext.asConfigBean(ImmutableOAuthConfiguration.class), sessionsService, accountsService);
    }

    public OAuthService(final ImmutableOAuthConfiguration configuration,
                        final SessionsService sessionsService,
                        final AccountsService accountsService) {
        this.sessionsService = sessionsService;

        this.providersClients = createClients(configuration.getClients());
        this.accountsService = accountsService;
        this.stateTtl = Duration.ofMinutes(5);
    }

    /**
     * Creates an authorization URL to be sent to the client to redirect
     * it to the identity provider authorization page. It generates a
     * new temporary session to store the state to be verified later.
     *
     * @param provider The name of a provider as stated in the configuration.
     */
    public CompletableFuture<String> getAuthorizationUrl(final String provider) {
        final OAuthServiceClient client = Optional.ofNullable(providersClients.get(provider))
                .orElseThrow(() -> new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE, "Invalid identity provider"));

        return CompletableFuture.supplyAsync(() -> sessionsService.create(SessionBO.builder()
                .expiresAt(OffsetDateTime.now().plus(stateTtl))
                .build()))
                .thenApply(session -> client.createAuthorizationUrl(session.getSessionToken(), ResponseType.CODE));
    }

    /**
     * Exchanges an authorization code with OAuth tokens. It'll verify that
     * a session containing that state exists before performing the exchange.
     * If the state has expired or no record of it existed then the future
     * will complete with {@link ServiceAuthorizationException}.
     *
     * @param provider The name of a provider as stated in the configuration.
     * @param state The state the identity provider returned.
     * @param authorizationCode The authorization code generated by the identity provider.
     */
    public CompletableFuture<TokensResponse> exchangeAuthorizationCode(final String provider, final String state,
                                                                       final String authorizationCode) {
        final OAuthServiceClient client = Optional.ofNullable(providersClients.get(provider))
                .orElseThrow(() -> new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE, "Invalid identity provider"));

        return CompletableFuture.supplyAsync(() -> sessionsService.getByToken(state))
                .thenCompose(sessionOptional -> sessionOptional
                        .map(session -> doExchange(client, authorizationCode, session))
                        .orElseThrow(() ->
                                new ServiceAuthorizationException(ErrorCode.TOKEN_EXPIRED_OR_DOES_NOT_EXIST,
                                        "The provided state is either invalid or has expired")))
                .thenApply(tokensResponse -> {
                    if (client.getConfiguration().isAccountProvider()) {
                        if (tokensResponse.getIdToken() == null) {
                            LOG.warn("Provider {} was set as an account provider but no ID was found in the response", provider);
                        } else {
                            final AccountBO account = getOrCreateAccount(client, authorizationCode, tokensResponse.getIdToken());

                            tokensResponse.setAccountId(account.getId());
                        }
                    }

                    return tokensResponse;
                });
    }

    private Map<String, OAuthServiceClient> createClients(final List<ImmutableOAuthClientConfiguration> clientsConfigs) {
        return clientsConfigs.stream()
                .map(clientConfig -> Maps.immutableEntry(clientConfig.getProvider(), new OAuthServiceClient(clientConfig)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private CompletableFuture<TokensResponse> doExchange(final OAuthServiceClient client,
                                                         final String authorizationCode,
                                                         final SessionBO session) {
        if (session.getExpiresAt().isAfter(OffsetDateTime.now())) {
            return client.authorize(authorizationCode);
        } else {
            throw new ServiceAuthorizationException(ErrorCode.TOKEN_EXPIRED_OR_DOES_NOT_EXIST,
                    "The provided state is either invalid or has expired");
        }
    }

    private AccountBO getOrCreateAccount(final OAuthServiceClient serviceClient, final String authorizationCode,
                                         final String idToken) {
        final ImmutableOAuthClientConfiguration configuration = serviceClient.getConfiguration();

        final DecodedJWT decoded = JWT.decode(idToken);
        final String externalId = decoded.getSubject();

        final Optional<AccountBO> account = accountsService.getByExternalId(externalId);

        if (account.isPresent()) {
            return account.get();
        }

        final AccountBO.Builder newAccount = AccountBO.builder()
                .externalId(externalId)
                .social(true)
                .identityProvider(configuration.getProvider());

        if (configuration.getEmailField() != null) {
            final Claim emailClaim = decoded.getClaim(configuration.getEmailField());

            if (!emailClaim.isNull()) {
                newAccount.email(AccountEmailBO.builder()
                        .email(emailClaim.asString())
                        .build());
            }
        }

        final RequestContextBO requestContext = RequestContextBO.builder()
                .source(configuration.getProvider())
                .idempotentKey(authorizationCode)
                .build();

        return accountsService.create(newAccount.build(), requestContext);
    }
}
