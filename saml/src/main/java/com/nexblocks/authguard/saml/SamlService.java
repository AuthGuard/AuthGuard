package com.nexblocks.authguard.saml;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.saml.config.ImmutableSamlConfiguration;
import com.nexblocks.authguard.saml.config.SamlConfiguration;
import com.nexblocks.authguard.saml.routes.SamlLoginRequest;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.TrackingSessionsService;
import com.nexblocks.authguard.service.config.ConfigParser;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.ServiceNotFoundException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import com.nexblocks.authguard.service.util.ID;
import io.smallrye.mutiny.Uni;
import io.vavr.control.Try;
import okhttp3.HttpUrl;
import org.opensaml.core.config.InitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public class SamlService {
    private static final Logger LOG = LoggerFactory.getLogger(SamlService.class);

    private static final String BASIC_TOKEN_TYPE = "basic";
    private static final String OTP_TOKEN_TYPE = "otp";
    private static final String SESSION_TOKEN_TYPE = "ssoSession";
    private static final String SAML_RESPONSE_TOKEN_TYPE = "samlResponse";
    private static final int REQUEST_TOKEN_SIZE = 64;
    private static final int SESSION_TOKEN_SIZE = 128;
    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);

    private final CryptographicRandom cryptographicRandom;
    private final TrackingSessionsService trackingSessionsService;
    private final ClientsService clientsService;
    private final ExchangeService exchangeService;
    private final AccountTokensRepository accountTokensRepository;
    private final Duration sessionLifetime;
    private final SamlConfiguration configuration;

    public static void initOpenSAML() {
        try {
            InitializationService.initialize();
        } catch (Exception e) {
            throw new RuntimeException("OpenSAML init failed", e);
        }
    }

    @Inject
    public SamlService(final CryptographicRandom cryptographicRandom,
                       final TrackingSessionsService trackingSessionsService,
                       final ClientsService clientsService,
                       final ExchangeService exchangeService,
                       final AccountTokensRepository accountTokensRepository,
                       final @Named("saml") ConfigContext configContext) {
        this(cryptographicRandom, trackingSessionsService, clientsService, exchangeService, accountTokensRepository,
                configContext.asConfigBean(ImmutableSamlConfiguration.class));
    }

    public SamlService(final CryptographicRandom cryptographicRandom,
                       final TrackingSessionsService trackingSessionsService,
                       final ClientsService clientsService,
                       final ExchangeService exchangeService,
                       final AccountTokensRepository accountTokensRepository,
                       final SamlConfiguration configuration) {
        this.cryptographicRandom = cryptographicRandom;
        this.trackingSessionsService = trackingSessionsService;
        this.clientsService = clientsService;
        this.exchangeService = exchangeService;
        this.accountTokensRepository = accountTokensRepository;
        this.configuration = configuration;

        if (configuration.getSessions() == null) {
            throw new ConfigurationException("Session configuration is required for SAML. " +
                    "Include 'saml.sessions' in your configuration");
        }

        this.sessionLifetime = ConfigParser.parseDuration(configuration.getSessions().getLifetime());
    }

    public Uni<Optional<Session>> getTrackingSessionIfActive(final String sessionToken,
                                                             final String domain) {
        return trackingSessionsService.getByToken(sessionToken)
                .map(opt -> opt.filter(session -> session.isActive()
                        && Objects.equals(session.getDomain(), domain)
                        && session.getExpiresAt().isAfter(Instant.now())));
    }

    public Uni<AccountTokenDO> createRequestToken(final RequestContextBO requestContext,
                                                  final SamlAuthnRequest request,
                                                  final String domain) {
        String token = cryptographicRandom.base64Url(REQUEST_TOKEN_SIZE);
        Map<String, String> parameters = new TreeMap<>();

        parameters.put(SamlConst.Params.RequestId, request.getRequestId());
        parameters.put(SamlConst.Params.RedirectUri, request.getAcsUrl());
        parameters.put(SamlConst.Params.RelayState, request.getRelayState());
        parameters.put(SamlConst.Params.Issuer, request.getIssuer());

        return clientsService.getByUri(request.getIssuer(), domain)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(
                                new ServiceException(ErrorCode.CLIENT_DOES_NOT_EXIST, "No client matching the given URI was found")
                        );
                    }

                    Client client = opt.get();

                    if (client.getClientType() != Client.ClientType.SSO) {
                        return Uni.createFrom().failure(
                                new ServiceException(ErrorCode.CLIENT_NOT_PERMITTED, "Client is not permitted for SSO")
                        );
                    }

                    return trackingSessionsService.startAnonymous(domain)
                            .flatMap(session -> {
                                AccountTokenDO accountToken = AccountTokenDO.builder()
                                        .id(ID.generate())
                                        .domain(domain)
                                        .token(token)
                                        .userAgent(requestContext.getUserAgent())
                                        .sourceIp(requestContext.getSource())
                                        .clientId(String.valueOf(client.getId()))
                                        .expiresAt(Instant.now().plus(TOKEN_TTL))
                                        .additionalInformation(parameters)
                                        .trackingSession(session.getSessionToken())
                                        .build();

                                return accountTokensRepository.save(accountToken);
                            });
                });
    }

    public Uni<SamlAuthnRequest> getRequestFromToken(final String token,
                                                     final RequestContextBO requestContext,
                                                     final String domain) {
        return accountTokensRepository.getByToken(token)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(new ServiceNotFoundException(ErrorCode.INVALID_TOKEN,
                                "invalid_token"));
                    }

                    AccountTokenDO accountToken = opt.get();

                    if (!Objects.equals(accountToken.getDomain(), domain)) {
                        return Uni.createFrom().failure(new ServiceNotFoundException(ErrorCode.INVALID_TOKEN,
                                "invalid_token"));
                    }

                    if (!Objects.equals(accountToken.getUserAgent(), requestContext.getUserAgent())) {
                        return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.GENERIC_AUTH_FAILURE,
                                "invalid_user_agent"));
                    }

                    Map<String, String> parameters = accountToken.getAdditionalInformation();

                    ImmutableSamlAuthnRequest request = ImmutableSamlAuthnRequest.builder()
                            .issuer(parameters.get(SamlConst.Params.Issuer))
                            .requestId(parameters.get(SamlConst.Params.RequestId))
                            .acsUrl(parameters.get(SamlConst.Params.RedirectUri))
                            .relayState(parameters.get(SamlConst.Params.RelayState))
                            .serverSideDetails(ImmutableServerSideDetails.builder()
                                    .clientId(accountToken.getClientId())
                                    .build())
                            .build();

                    return Uni.createFrom().item(request);
                });
    }

    public Uni<AccountTokenDO> startSession(final String trackingSessionToken,
                                            final long accountId,
                                            final Client client,
                                            final String source) {
        AccountTokenDO sessionToken = AccountTokenDO.builder()
                .id(ID.generate())
                .associatedAccountId(accountId)
                .token(cryptographicRandom.base64(SESSION_TOKEN_SIZE))
                .trackingSession(trackingSessionToken)
                .clientId("" + client.getId())
                .sourceAuthType(source)
                .expiresAt(Instant.now().plus(sessionLifetime))
                .build();

        return accountTokensRepository.save(sessionToken);
    }

    public Uni<AuthResponseBO> processAuthBasicToOtp(final SamlAuthnRequest originalRequest,
                                                     final SamlLoginRequest loginRequest,
                                                     final RequestContextBO requestContext,
                                                     final String domain) {
        long parsedId;

        try {
            parsedId = Long.parseLong(originalRequest.getServerSideDetails().getClientId());
        } catch (Exception e) {
            return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.APP_DOES_NOT_EXIST,
                    "Invalid client ID"));
        }

        return getClientIfValid(parsedId, domain)
                .flatMap(client -> {
                    Try<AuthRequestBO> validatedRequest = verifyClientRequest(client, originalRequest.getAcsUrl())
                            .map(ignored -> createRequest(loginRequest, originalRequest, client));

                    if (validatedRequest.isFailure()) {
                        return Uni.createFrom().failure(validatedRequest.getCause());
                    }

                    return exchangeService.exchange(validatedRequest.get(),
                                    BASIC_TOKEN_TYPE, OTP_TOKEN_TYPE, requestContext)
                            .map(response -> response.withClient(client));
                });
    }

    public Uni<AuthResponseBO> processSessionToSamlResponse(final SamlAuthnRequest originalRequest,
                                                            final String sessionToken,
                                                            final SamlLoginRequest loginRequest,
                                                            final RequestContextBO requestContext,
                                                            final String domain) {
        if (originalRequest.isForceAuthn()) {
            return Uni.createFrom().failure(
                    new ServiceException(ErrorCode.SESSION_DOES_NOT_MEET_REQUIREMENTS, "Request requires authenticating the user")
            );
        }

        long parsedId;

        try {
            parsedId = Long.parseLong(originalRequest.getServerSideDetails().getClientId());
        } catch (Exception e) {
            return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.APP_DOES_NOT_EXIST,
                    "Invalid client ID"));
        }

        return clientsService.getById(parsedId, domain)
                .flatMap(opt -> opt
                        .map(item -> Uni.createFrom().item(item))
                        .orElseGet(() -> Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.APP_DOES_NOT_EXIST, "Client does not exist"))))
                .flatMap(client -> {
                    if (client.getClientType() != Client.ClientType.SSO) {
                        return Uni.createFrom().failure(new ServiceException(ErrorCode.CLIENT_NOT_PERMITTED,
                                "Client isn't permitted to perform SAML requests"));
                    }

                    Try<AuthRequestBO> validatedRequest = verifyClientRequest(client, originalRequest.getAcsUrl())
                            .map(ignored -> createRequestFromSession(sessionToken, loginRequest, originalRequest, client));

                    if (validatedRequest.isFailure()) {
                        return Uni.createFrom().failure(validatedRequest.getCause());
                    }

                    return exchangeService
                            .exchange(validatedRequest.get(), SESSION_TOKEN_TYPE, SAML_RESPONSE_TOKEN_TYPE, requestContext)
                            .map(response -> response.withClient(client))
                            .flatMap(response -> refreshSessionIfAllowed(sessionToken, response));
                });
    }

    public Uni<AuthResponseBO> processAuthBasicToSamlResponse(final SamlAuthnRequest originalRequest,
                                                              final SamlLoginRequest loginRequest,
                                                              final RequestContextBO requestContext,
                                                              final String domain) {
        long parsedId;

        try {
            parsedId = Long.parseLong(originalRequest.getServerSideDetails().getClientId());
        } catch (Exception e) {
            return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.APP_DOES_NOT_EXIST,
                    "Invalid client ID"));
        }

        return clientsService.getById(parsedId, domain)
                .flatMap(opt -> opt
                        .map(item -> Uni.createFrom().item(item))
                        .orElseGet(() -> Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.APP_DOES_NOT_EXIST, "Client does not exist"))))
                .flatMap(client -> {
                    if (client.getClientType() != Client.ClientType.SSO) {
                        return Uni.createFrom().failure(new ServiceException(ErrorCode.CLIENT_NOT_PERMITTED,
                                "Client isn't permitted to perform SAML requests"));
                    }

                    Try<AuthRequestBO> validatedRequest = verifyClientRequest(client, originalRequest.getAcsUrl())
                            .map(ignored -> createRequest(loginRequest, originalRequest, client));

                    if (validatedRequest.isFailure()) {
                        return Uni.createFrom().failure(validatedRequest.getCause());
                    }

                    return exchangeService.exchange(validatedRequest.get(), BASIC_TOKEN_TYPE, SAML_RESPONSE_TOKEN_TYPE, requestContext)
                            .flatMap(response -> tieResponseToSession(response, client));
                });
    }

    public Uni<AuthResponseBO> processAuthOtpToSamlResponse(final SamlAuthnRequest originalRequest,
                                                            final SamlLoginRequest loginRequest,
                                                            final RequestContextBO requestContext,
                                                            final String domain) {
        long parsedId;

        try {
            parsedId = Long.parseLong(originalRequest.getServerSideDetails().getClientId());
        } catch (Exception e) {
            return Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.APP_DOES_NOT_EXIST,
                    "Invalid client ID"));
        }

        return clientsService.getById(parsedId, domain)
                .flatMap(opt -> opt
                        .map(item -> Uni.createFrom().item(item))
                        .orElseGet(() -> Uni.createFrom().failure(new ServiceAuthorizationException(ErrorCode.APP_DOES_NOT_EXIST, "Client does not exist"))))
                .flatMap(client -> {
                    if (client.getClientType() != Client.ClientType.SSO) {
                        return Uni.createFrom().failure(new ServiceException(ErrorCode.CLIENT_NOT_PERMITTED,
                                "Client isn't permitted to perform SAML requests"));
                    }

                    Try<AuthRequestBO> validatedRequest = verifyClientRequest(client, originalRequest.getAcsUrl())
                            .map(ignored -> createRequest(loginRequest, originalRequest, client))
                            .map(authRequest -> authRequest.withToken(authRequest.getIdentifier() + ":" + authRequest.getPassword()));;

                    if (validatedRequest.isFailure()) {
                        return Uni.createFrom().failure(validatedRequest.getCause());
                    }

                    return exchangeService.exchange(validatedRequest.get(), OTP_TOKEN_TYPE, SAML_RESPONSE_TOKEN_TYPE, requestContext)
                            .flatMap(response -> tieResponseToSession(response, client));
                });
    }

    private AuthRequestBO createRequest(final SamlLoginRequest loginRequest,
                                        final SamlAuthnRequest originalAuthnRequest,
                                        final ClientBO client) {
        return AuthRequestBO.builder()
                .domain(client.getDomain())
                .identifier(loginRequest.getIdentifier())
                .password(loginRequest.getPassword())
                .trackingSession(loginRequest.getTrackingSession())
                .extraParameters(originalAuthnRequest)
                .build();
    }

    private AuthRequestBO createRequestFromSession(final String sessionToken,
                                                   final SamlLoginRequest loginRequest,
                                                   final SamlAuthnRequest originalAuthnRequest,
                                                   final ClientBO client) {
        return AuthRequestBO.builder()
                .domain(client.getDomain())
                .token(sessionToken)
                .trackingSession(loginRequest.getTrackingSession())
                .extraParameters(originalAuthnRequest)
                .build();
    }

    private Uni<AuthResponseBO> tieResponseToSession(final AuthResponseBO response,
                                                     final ClientBO client) {
        return startSession(response.getTrackingSession(), response.getEntityId(), client, BASIC_TOKEN_TYPE)
                .map(session -> response.withAuthSessionToken(session.getToken())
                        .withClient(client));
    }

    public Uni<AuthResponseBO> refreshSessionIfAllowed(final String sessionToken,
                                                       final AuthResponseBO response) {
        if (!configuration.getSessions().allowRefresh()) {
            LOG.debug("Refresh is not allowed");
            return Uni.createFrom().item(response);
        }

        return accountTokensRepository.getByToken(sessionToken)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        LOG.error("Failed to refresh SSO session: session does not exist. This will not fail the request.");
                        return Uni.createFrom().item(response);
                    }

                    AccountTokenDO session = opt.get();

                    AccountTokenDO newSession = AccountTokenDO.builder()
                            .id(session.getId())
                            .associatedAccountId(session.getAssociatedAccountId())
                            .token(session.getToken())
                            .trackingSession(session.getTrackingSession())
                            .clientId(session.getClientId())
                            .sourceAuthType(session.getSourceAuthType())
                            .expiresAt(Instant.now().plus(sessionLifetime))
                            .build();

                    LOG.debug("Refreshing session id={}, oldExpiryTime={}, newExpiryTime={}",
                            session.getId(), session.getExpiresAt(), newSession.getExpiresAt());

                    return accountTokensRepository.save(newSession)
                            .flatMap(ignored -> Uni.createFrom().item(response));
                });
    }

    // TODO way too much replication here, move to service layer
    private Try<Boolean> verifyClientRequest(Client client, String redirectUri) {
        if (client.getClientType() != Client.ClientType.SSO) {
            return Try.failure(new ServiceException(ErrorCode.CLIENT_NOT_PERMITTED,
                    "Client isn't permitted to perform SAML requests"));
        }

        HttpUrl parsedUrl = HttpUrl.parse(redirectUri);

        if (parsedUrl == null) {
            return Try.failure(new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Invalid redirect URL"));
        }

        if (!AcsUrlValidator.isValidAcsUrl(parsedUrl, client.getBaseUrl())) {
            return Try.failure(new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Redirect URL doesn't match the client URL"));
        }

        return Try.success(true);
    }

    private Uni<ClientBO> getClientIfValid(long clientId, String domain) {
        return clientsService.getById(clientId, domain)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        return Uni.createFrom().failure(
                                new ServiceAuthorizationException(ErrorCode.APP_DOES_NOT_EXIST,
                                        "Client does not exist")
                        );
                    }

                    ClientBO client = opt.get();

                    if (client.getClientType() != Client.ClientType.SSO) {
                        return Uni.createFrom().failure(new ServiceException(ErrorCode.CLIENT_NOT_PERMITTED,
                                "Client isn't permitted to perform SAML requests"));
                    }

                    return Uni.createFrom().item(client);
                });
    }
}
