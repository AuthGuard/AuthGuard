package com.nexblocks.authguard.saml;

import com.google.common.collect.ImmutableMap;
import com.nexblocks.authguard.dal.cache.AccountTokensRepository;
import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.saml.config.ImmutableSamlConfiguration;
import com.nexblocks.authguard.saml.config.ImmutableSamlSsoSession;
import com.nexblocks.authguard.service.ClientsService;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.TrackingSessionsService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.*;
import com.nexblocks.authguard.service.random.CryptographicRandom;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.nexblocks.authguard.saml.routes.ImmutableSamlLoginRequest;
import com.nexblocks.authguard.saml.routes.SamlLoginRequest;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class SamlServiceTest {
    private static final String DOMAIN = "main";

    private TrackingSessionsService trackingSessionsService;
    private ClientsService clientsService;
    private ExchangeService exchangeService;
    private AccountTokensRepository accountTokensRepository;

    private SamlService samlService;

    @BeforeEach
    void setup() {
        trackingSessionsService = mock(TrackingSessionsService.class);
        clientsService = mock(ClientsService.class);
        exchangeService = mock(ExchangeService.class);
        accountTokensRepository = mock(AccountTokensRepository.class);

        samlService = new SamlService(new CryptographicRandom(),
                trackingSessionsService,
                clientsService,
                exchangeService,
                accountTokensRepository,
                ImmutableSamlConfiguration.builder()
                        .sessions(ImmutableSamlSsoSession.builder()
                                .lifetime("60m")
                                .build())
                        .build());
    }

    @Test
    void getTrackingSessionIfActive() {
        String token = "session-token";
        Session session = SessionBO.builder()
                .sessionToken(token)
                .active(true)
                .expiresAt(Instant.now().plusSeconds(2))
                .domain(DOMAIN)
                .build();

        when(trackingSessionsService.getByToken(token))
                .thenReturn(Uni.createFrom().item(Optional.of(session)));

        Optional<Session> result = samlService.getTrackingSessionIfActive(token, DOMAIN)
                .subscribeAsCompletionStage()
                .join();

        assertThat(result).contains(session);
    }

    @Test
    void getSessionIfActiveInactiveTrackingSession() {
        String token = "session-token";
        Session session = SessionBO.builder()
                .sessionToken(token)
                .active(false)
                .expiresAt(Instant.now().plusSeconds(2))
                .domain(DOMAIN)
                .build();

        when(trackingSessionsService.getByToken(token))
                .thenReturn(Uni.createFrom().item(Optional.of(session)));

        Optional<Session> result = samlService.getTrackingSessionIfActive(token, DOMAIN)
                .subscribeAsCompletionStage()
                .join();

        assertThat(result).isEmpty();
    }

    @Test
    void getSessionIfActiveExpiredTrackingSession() {
        String token = "session-token";
        Session session = SessionBO.builder()
                .sessionToken(token)
                .active(true)
                .expiresAt(Instant.now().minusSeconds(2))
                .domain(DOMAIN)
                .build();

        when(trackingSessionsService.getByToken(token))
                .thenReturn(Uni.createFrom().item(Optional.of(session)));

        Optional<Session> result = samlService.getTrackingSessionIfActive(token, DOMAIN)
                .subscribeAsCompletionStage()
                .join();

        assertThat(result).isEmpty();
    }

    @Test
    void getTrackingSessionIfActiveDifferentDomain() {
        String token = "session-token";
        Session session = SessionBO.builder()
                .sessionToken(token)
                .active(true)
                .expiresAt(Instant.now().plusSeconds(2))
                .domain("other")
                .build();

        when(trackingSessionsService.getByToken(token))
                .thenReturn(Uni.createFrom().item(Optional.of(session)));

        Optional<Session> result = samlService.getTrackingSessionIfActive(token, DOMAIN)
                .subscribeAsCompletionStage()
                .join();

        assertThat(result).isEmpty();
    }

    @Test
    void createRequestToken() {
        String clientUri = "urn:test-client";
        String sessionToken = "session-token";

        ClientBO client = ClientBO.builder()
                .id(1)
                .clientType(Client.ClientType.SSO)
                .uri(clientUri)
                .build();

        Session trackingSession = SessionBO.builder()
                .domain(DOMAIN)
                .sessionToken(sessionToken)
                .build();

        RequestContextBO requestContext = RequestContextBO.builder()
                .userAgent("tests")
                .source("127.0.0.1")
                .build();

        SamlAuthnRequest samlAuthnRequest = ImmutableSamlAuthnRequest.builder()
                .issuer(clientUri)
                .requestId("request-id")
                .acsUrl("http://localhost:8080")
                .relayState("state")
                .build();

        when(clientsService.getByUri(clientUri, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        when(trackingSessionsService.startAnonymous(DOMAIN))
                .thenReturn(Uni.createFrom().item(trackingSession));

        when(accountTokensRepository.save(any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, AccountTokenDO.class)));

        AccountTokenDO result = samlService.createRequestToken(requestContext, samlAuthnRequest, DOMAIN)
                .subscribeAsCompletionStage()
                .join();

        AccountTokenDO expected = AccountTokenDO.builder()
                .domain(DOMAIN)
                .clientId("1")
                .userAgent("tests")
                .sourceIp("127.0.0.1")
                .trackingSession(sessionToken)
                .additionalInformation(ImmutableMap.<String, String>builder()
                        .put(SamlConst.Params.Issuer, clientUri)
                        .put(SamlConst.Params.RequestId, samlAuthnRequest.getRequestId())
                        .put(SamlConst.Params.RedirectUri, samlAuthnRequest.getAcsUrl())
                        .put(SamlConst.Params.RelayState, samlAuthnRequest.getRelayState())
                        .build())
                .build();

        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("id", "token", "expiresAt")
                .isEqualTo(expected);
    }

    @Test
    void createRequestTokenNonSsoClient() {
        String clientUri = "urn:test-client";

        ClientBO client = ClientBO.builder()
                .id(1)
                .clientType(Client.ClientType.ADMIN)
                .uri(clientUri)
                .build();

        RequestContextBO requestContext = RequestContextBO.builder()
                .userAgent("tests")
                .source("127.0.0.1")
                .build();

        SamlAuthnRequest samlAuthnRequest = ImmutableSamlAuthnRequest.builder()
                .issuer(clientUri)
                .requestId("request-id")
                .acsUrl("http://localhost:8080")
                .relayState("state")
                .build();

        when(clientsService.getByUri(clientUri, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        CompletableFuture<AccountTokenDO> future = samlService.createRequestToken(requestContext, samlAuthnRequest, DOMAIN)
                .subscribeAsCompletionStage();

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class);

        // TODO assert error code
    }

    @Test
    void createRequestTokenClientDoesNotExist() {
        String clientUri = "urn:test-client";

        RequestContextBO requestContext = RequestContextBO.builder()
                .userAgent("tests")
                .source("127.0.0.1")
                .build();

        SamlAuthnRequest samlAuthnRequest = ImmutableSamlAuthnRequest.builder()
                .issuer(clientUri)
                .requestId("request-id")
                .acsUrl("http://localhost:8080")
                .relayState("state")
                .build();

        when(clientsService.getByUri(clientUri, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        CompletableFuture<AccountTokenDO> future = samlService.createRequestToken(requestContext, samlAuthnRequest, DOMAIN)
                .subscribeAsCompletionStage();

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class);

        // TODO assert error code
    }

    @Test
    void getRequestFromToken() {
    }

    @Test
    void processAuthBasicToSamlResponseNonSsoClient() {
        long clientId = 1L;
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .clientType(Client.ClientType.ADMIN)
                .domain(DOMAIN)
                .baseUrl("https://www.sp.com/callback")
                .build();

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl("https://www.sp.com/callback")
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));
        
        CompletableFuture<AuthResponseBO> future = samlService.processAuthBasicToSamlResponse(originalRequest, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage();
        
        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void processAuthBasicToSamlResponseClientDoesNotExist() {
        long clientId = 1L;

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl("https://www.sp.com/callback")
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.empty()));
        
        CompletableFuture<AuthResponseBO> future = samlService.processAuthBasicToSamlResponse(originalRequest, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage();
        
        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException.class);
    }

    @Test
    void processAuthBasicToSamlResponseInvalidRedirectUrl() {
        long clientId = 1L;
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .clientType(Client.ClientType.SSO)
                .domain(DOMAIN)
                .baseUrl("https://www.sp.com/callback")
                .build();

        String invalidAcs = "nothttp://\\invalid";

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl(invalidAcs)
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));
        
        CompletableFuture<AuthResponseBO> future = samlService.processAuthBasicToSamlResponse(originalRequest, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage();
        
        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void processAuthBasicToSamlResponseInvalidAcsUrl() {
        long clientId = 1L;
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .clientType(Client.ClientType.SSO)
                .domain(DOMAIN)
                .baseUrl("https://www.sp.com/callback")
                .build();

        // URL that doesn't match client's baseUrl
        String mismatchingAcs = "https://evil.example.com/callback";

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl(mismatchingAcs)
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));
        
        CompletableFuture<AuthResponseBO> future = samlService.processAuthBasicToSamlResponse(originalRequest, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage();
        
        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void processAuthBasicToSamlResponse() {
        long clientId = 1L;
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .clientType(Client.ClientType.SSO)
                .domain(DOMAIN)
                .baseUrl("https://www.sp.com/callback")
                .build();

        String acs = "https://www.sp.com/callback";

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl(acs)
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        String basicType = "Basic";
        AuthResponseBO response = AuthResponseBO.builder()
                .type(basicType)
                .token("dummy")
                .entityType(EntityType.ACCOUNT)
                .entityId(101)
                .build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));
        when(exchangeService.exchange(any(AuthRequestBO.class), eq("basic"), eq("samlResponse"), same(requestContext)))
                .thenReturn(Uni.createFrom().item(response));
        when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, AccountTokenDO.class)));
        
        AuthResponseBO result = samlService.processAuthBasicToSamlResponse(originalRequest, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage()
                .join();
        
        assertThat(result).isNotNull();
        assertThat(result.getClient()).isEqualTo(client);

        ArgumentCaptor<AuthRequestBO> requestCaptor = ArgumentCaptor.forClass(AuthRequestBO.class);
        verify(exchangeService).exchange(requestCaptor.capture(), eq("basic"), eq("samlResponse"), same(requestContext));
        
        AuthRequestBO captured = requestCaptor.getValue();
        assertThat(captured.getDomain()).isEqualTo(DOMAIN);
        assertThat(captured.getIdentifier()).isEqualTo("user@example.com");
        assertThat(captured.getPassword()).isEqualTo("secret");
        assertThat(captured.getTrackingSession()).isEqualTo("track-1");
        assertThat(captured.getExtraParameters()).isEqualTo(originalRequest);
    }

    @Test
    void processAuthOtpToSamlResponseNonSsoClient() {
        long clientId = 1L;
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .clientType(Client.ClientType.ADMIN)
                .domain(DOMAIN)
                .baseUrl("https://www.sp.com/callback")
                .build();

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl("https://www.sp.com/callback")
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        CompletableFuture<AuthResponseBO> future = samlService.processAuthOtpToSamlResponse(originalRequest, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage();

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void processAuthOtpToSamlResponseClientDoesNotExist() {
        long clientId = 1L;

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl("https://www.sp.com/callback")
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        CompletableFuture<AuthResponseBO> future = samlService.processAuthOtpToSamlResponse(originalRequest, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage();

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException.class);
    }

    @Test
    void processAuthOtpToSamlResponseInvalidRedirectUrl() {
        long clientId = 1L;
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .clientType(Client.ClientType.SSO)
                .domain(DOMAIN)
                .baseUrl("https://www.sp.com/callback")
                .build();

        String invalidAcs = "nothttp://\\invalid";

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl(invalidAcs)
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        CompletableFuture<AuthResponseBO> future = samlService.processAuthOtpToSamlResponse(originalRequest, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage();

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void processAuthOtpToSamlResponseInvalidAcsUrl() {
        long clientId = 1L;
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .clientType(Client.ClientType.SSO)
                .domain(DOMAIN)
                .baseUrl("https://www.sp.com/callback")
                .build();

        // URL that doesn't match client's baseUrl
        String mismatchingAcs = "https://evil.example.com/callback";

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl(mismatchingAcs)
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        CompletableFuture<AuthResponseBO> future = samlService.processAuthOtpToSamlResponse(originalRequest, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage();

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void processAuthOtpToSamlResponse() {
        long clientId = 1L;
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .clientType(Client.ClientType.SSO)
                .domain(DOMAIN)
                .baseUrl("https://www.sp.com/callback")
                .build();

        String acs = "https://www.sp.com/callback";

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl(acs)
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        String otpType = "Otp";
        AuthResponseBO response = AuthResponseBO.builder()
                .type(otpType)
                .token("dummy")
                .entityType(EntityType.ACCOUNT)
                .entityId(101)
                .build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));
        when(exchangeService.exchange(any(AuthRequestBO.class), eq("otp"), eq("samlResponse"), same(requestContext)))
                .thenReturn(Uni.createFrom().item(response));
        when(accountTokensRepository.save(Mockito.any()))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, AccountTokenDO.class)));

        AuthResponseBO result = samlService.processAuthOtpToSamlResponse(originalRequest, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage()
                .join();

        assertThat(result).isNotNull();
        assertThat(result.getClient()).isEqualTo(client);

        ArgumentCaptor<AuthRequestBO> requestCaptor = ArgumentCaptor.forClass(AuthRequestBO.class);
        verify(exchangeService).exchange(requestCaptor.capture(), eq("otp"), eq("samlResponse"), same(requestContext));

        AuthRequestBO captured = requestCaptor.getValue();
        assertThat(captured.getDomain()).isEqualTo(DOMAIN);
        assertThat(captured.getIdentifier()).isEqualTo("user@example.com");
        assertThat(captured.getPassword()).isEqualTo("secret");
        assertThat(captured.getTrackingSession()).isEqualTo("track-1");
        assertThat(captured.getExtraParameters()).isEqualTo(originalRequest);
    }

    @Test
    void processAuthBasicToOtpNonSsoClient() {
        long clientId = 1L;
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .clientType(Client.ClientType.ADMIN)
                .domain(DOMAIN)
                .baseUrl("https://www.sp.com/callback")
                .build();

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl("https://www.sp.com/callback")
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        CompletableFuture<AuthResponseBO> future = samlService.processAuthBasicToOtp(originalRequest, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage();

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void processAuthBasicToOtpClientDoesNotExist() {
        long clientId = 1L;

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl("https://www.sp.com/callback")
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        CompletableFuture<AuthResponseBO> future = samlService.processAuthBasicToOtp(originalRequest, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage();

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException.class);
    }

    @Test
    void processAuthBasicToOtpInvalidRedirectUrl() {
        long clientId = 1L;
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .clientType(Client.ClientType.SSO)
                .domain(DOMAIN)
                .baseUrl("https://www.sp.com/callback")
                .build();

        String invalidAcs = "nothttp://\\invalid";

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl(invalidAcs)
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        CompletableFuture<AuthResponseBO> future = samlService.processAuthBasicToOtp(originalRequest, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage();

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void processAuthBasicToOtpInvalidAcsUrl() {
        long clientId = 1L;
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .clientType(Client.ClientType.SSO)
                .domain(DOMAIN)
                .baseUrl("https://www.sp.com/callback")
                .build();

        // URL that doesn't match client's baseUrl
        String mismatchingAcs = "https://evil.example.com/callback";

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl(mismatchingAcs)
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        CompletableFuture<AuthResponseBO> future = samlService.processAuthBasicToOtp(originalRequest, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage();

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class);
    }

    @Test
    void processAuthBasicToOtp() {
        long clientId = 1L;
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .clientType(Client.ClientType.SSO)
                .domain(DOMAIN)
                .baseUrl("https://www.sp.com/callback")
                .build();

        String acs = "https://www.sp.com/callback";

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl(acs)
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        String basicType = "Basic";
        AuthResponseBO response = AuthResponseBO.builder()
                .type(basicType)
                .token("dummy")
                .entityType(EntityType.ACCOUNT)
                .entityId(101)
                .build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));
        when(exchangeService.exchange(any(AuthRequestBO.class), eq("basic"), eq("otp"), same(requestContext)))
                .thenReturn(Uni.createFrom().item(response));

        AuthResponseBO result = samlService.processAuthBasicToOtp(originalRequest, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage()
                .join();

        assertThat(result).isNotNull();
        assertThat(result.getClient()).isEqualTo(client);

        ArgumentCaptor<AuthRequestBO> requestCaptor = ArgumentCaptor.forClass(AuthRequestBO.class);
        verify(exchangeService).exchange(requestCaptor.capture(), eq("basic"), eq("otp"), same(requestContext));

        AuthRequestBO captured = requestCaptor.getValue();
        assertThat(captured.getDomain()).isEqualTo(DOMAIN);
        assertThat(captured.getIdentifier()).isEqualTo("user@example.com");
        assertThat(captured.getPassword()).isEqualTo("secret");
        assertThat(captured.getTrackingSession()).isEqualTo("track-1");
        assertThat(captured.getExtraParameters()).isEqualTo(originalRequest);
    }

    @Test
    void processSessionToSamlResponseForceAuthn() {
        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl("https://www.sp.com/callback")
                .forceAuthn(true)
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId("1").build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        CompletableFuture<AuthResponseBO> future = samlService.processSessionToSamlResponse(
                originalRequest, "session-token", loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage();

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class)
                .cause()
                .satisfies(e -> {
                    ServiceException se = (ServiceException) e;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.SESSION_DOES_NOT_MEET_REQUIREMENTS.getCode());
                });
    }

    @Test
    void processSessionToSamlResponseClientDoesNotExist() {
        long clientId = 1L;

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl("https://www.sp.com/callback")
                .forceAuthn(false)
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.empty()));

        CompletableFuture<AuthResponseBO> future = samlService.processSessionToSamlResponse(
                originalRequest, "session-token", loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage();

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(com.nexblocks.authguard.service.exceptions.ServiceAuthorizationException.class);
    }

    @Test
    void processSessionToSamlResponseNonSsoClient() {
        long clientId = 1L;
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .clientType(Client.ClientType.ADMIN)
                .domain(DOMAIN)
                .baseUrl("https://www.sp.com/callback")
                .build();

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl("https://www.sp.com/callback")
                .forceAuthn(false)
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));

        CompletableFuture<AuthResponseBO> future = samlService.processSessionToSamlResponse(
                originalRequest, "session-token", loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage();

        assertThatThrownBy(future::join)
                .hasCauseInstanceOf(ServiceException.class)
                .cause()
                .satisfies(e -> {
                    ServiceException se = (ServiceException) e;
                    assertThat(se.getErrorCode()).isEqualTo(ErrorCode.CLIENT_NOT_PERMITTED.getCode());
                });
    }

    @Test
    void processSessionToSamlResponse() {
        long clientId = 1L;
        ClientBO client = ClientBO.builder()
                .id(clientId)
                .clientType(Client.ClientType.SSO)
                .domain(DOMAIN)
                .baseUrl("https://www.sp.com/callback")
                .build();

        String acs = "https://www.sp.com/callback";
        String sessionToken = "session-token";

        SamlAuthnRequest originalRequest = ImmutableSamlAuthnRequest.builder()
                .issuer("urn:test-client")
                .requestId("request-id")
                .acsUrl(acs)
                .forceAuthn(false)
                .serverSideDetails(ImmutableServerSideDetails.builder().clientId(String.valueOf(clientId)).build())
                .build();

        SamlLoginRequest loginRequest = ImmutableSamlLoginRequest.builder()
                .identifier("user@example.com")
                .password("secret")
                .trackingSession("track-1")
                .requestToken("token-1")
                .build();

        RequestContextBO requestContext = RequestContextBO.builder().build();

        String sessionType = "ssoSession";
        AuthResponseBO response = AuthResponseBO.builder()
                .type(sessionType)
                .token("dummy")
                .entityType(EntityType.ACCOUNT)
                .entityId(101)
                .build();

        when(clientsService.getById(clientId, DOMAIN))
                .thenReturn(Uni.createFrom().item(Optional.of(client)));
        when(exchangeService.exchange(any(AuthRequestBO.class), eq("ssoSession"), eq("samlResponse"), same(requestContext)))
                .thenReturn(Uni.createFrom().item(response));

        AuthResponseBO result = samlService.processSessionToSamlResponse(
                originalRequest, sessionToken, loginRequest, requestContext, DOMAIN)
                .subscribeAsCompletionStage()
                .join();

        assertThat(result).isNotNull();
        assertThat(result.getClient()).isEqualTo(client);

        verify(exchangeService).exchange(any(AuthRequestBO.class), eq("ssoSession"), eq("samlResponse"), same(requestContext));
    }
}