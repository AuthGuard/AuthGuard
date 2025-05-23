package com.nexblocks.authguard.rest.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import com.google.inject.Inject;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.api.common.RequestContextExtractor;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.entities.AuthResponseDTO;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.entities.ExchangeAttemptDTO;
import com.nexblocks.authguard.api.dto.requests.AuthRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.AuthApi;
import com.nexblocks.authguard.rest.access.ActorDomainVerifier;
import com.nexblocks.authguard.rest.access.Requester;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.AuthenticationService;
import com.nexblocks.authguard.service.ExchangeAttemptsService;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.*;
import io.javalin.http.Context;
import io.javalin.validation.Validator;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.post;

public class AuthRoute extends AuthApi {
    private final AuthenticationService authenticationService;
    private final ExchangeService exchangeService;
    private final ExchangeAttemptsService exchangeAttemptsService;
    private final RestMapper restMapper;

    private final BodyHandler<AuthRequestDTO> authRequestBodyHandler;

    @Inject
    AuthRoute(final AuthenticationService authenticationService, final ExchangeService exchangeService,
              final ExchangeAttemptsService exchangeAttemptsService, final RestMapper restMapper) {
        this.authenticationService = authenticationService;
        this.exchangeService = exchangeService;
        this.exchangeAttemptsService = exchangeAttemptsService;
        this.restMapper = restMapper;

        this.authRequestBodyHandler = new BodyHandler.Builder<>(AuthRequestDTO.class)
                .build();
    }

    @Override
    public String getPath() {
        return "/domains/{domain}/auth";
    }

    @Override
    public void addEndpoints() {
        post("/authenticate", this::authenticate, ActorRoles.adminOrAuthClient());
        post("/logout", this::logout, ActorRoles.adminOrAuthClient());
        post("/refresh", this::refresh, ActorRoles.adminOrAuthClient());
        post("/exchange", this::exchange, ActorRoles.adminClient());
        post("/exchange/clear", this::clearToken, ActorRoles.adminClient());
        get("/exchange/attempts", this::getExchangeAttempts, ActorRoles.adminClient());
    }

    @Override
    public void authenticate(final Context context) {
        Optional<AuthRequestDTO> authRequest = getValidRequestOrFail(context);

        if (authRequest.isEmpty()) {
            return;
        }

        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);
        AuthRequestBO bo = restMapper.toBO(authRequest.get());

        CompletableFuture<AuthResponseDTO> tokens = authenticationService
                .authenticate(bo, requestContext)
                .thenApply(restMapper::toDTO);

        context.future(() -> tokens.thenAccept(context::json));
    }

    @Override
    public void logout(final Context context) {
        AuthRequestDTO authenticationRequest = authRequestBodyHandler.getValidated(context);
        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        CompletableFuture<AuthResponseBO> result = authenticationService.logout(
                restMapper.toBO(authenticationRequest), requestContext);

        context.future(() -> result.thenAccept(context::json));
    }

    @Override
    public void refresh(final Context context) {
        Optional<AuthRequestDTO> authRequest = getValidRequestOrFail(context);

        if (authRequest.isEmpty()) {
            return;
        }

        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        CompletableFuture<AuthResponseDTO> tokens = authenticationService.refresh(restMapper.toBO(authRequest.get()), requestContext)
                .thenApply(restMapper::toDTO);

        context.future(() -> tokens.thenAccept(context::json));
    }

    @Override
    public void exchange(final Context context) {
        Optional<AuthRequestDTO> authRequest = getValidRequestOrFail(context);

        if (authRequest.isEmpty()) {
            return;
        }

        String from = context.queryParam("from");
        String to = context.queryParam("to");

        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        CompletableFuture<AuthResponseDTO> tokens = exchangeService.exchange(restMapper.toBO(authRequest.get()), from, to, requestContext)
                .thenApply(restMapper::toDTO);

        context.future(() -> tokens.thenAccept(context::json));
    }

    @Override
    public void clearToken(final Context context) {
        AuthRequestDTO authenticationRequest = authRequestBodyHandler.getValidated(context);
        String tokenType = context.queryParam("tokenType");

        if (tokenType == null) {
            context.status(400)
                    .json(new Error("400", "Missing 'tokenType' query parameter"));
        } else {
            CompletableFuture<AuthResponseDTO> tokens = exchangeService.delete(restMapper.toBO(authenticationRequest), tokenType)
                    .thenApply(restMapper::toDTO);

            context.future(() -> tokens.thenAccept(context::json));
        }
    }

    @Override
    public void getExchangeAttempts(final Context context) {
        Validator<Long> entityId = context.queryParamAsClass("entityId", Long.class);
        Instant fromTimestamp = parseOffsetDateTime(context.queryParam("fromTimestamp"));
        String fromExchange = context.queryParam("fromExchange");

        // take care of checking the parameters
        if (entityId.getOrDefault(null) == null) {
            context.status(400)
                    .json(new Error(ErrorCode.MISSING_REQUEST_QUERY.getCode(),
                            "Query parameter entityId is required"));

            return;
        }

        if (fromExchange != null && fromTimestamp == null) {
            context.status(400)
                    .json(new Error(ErrorCode.MISSING_REQUEST_QUERY.getCode(),
                            "Query parameter fromTimestamp is required when fromExchange is set"));

            return;
        }

        // do the real work
        ExchangeAttemptsQueryBO query = ExchangeAttemptsQueryBO.builder()
                .entityId(entityId.get())
                .fromTimestamp(fromTimestamp)
                .fromExchange(fromExchange)
                .build();

        Collection<ExchangeAttemptDTO> attempts = exchangeAttemptsService.find(query)
                .stream()
                .map(restMapper::toDTO)
                .collect(Collectors.toList());

        context.json(attempts);
    }

    private Optional<AuthRequestDTO> getValidRequestOrFail(final Context context) {
        AuthRequestDTO authRequest = authRequestBodyHandler.getValidated(context);

        Optional<ClientBO> actor = Requester.getIfApp(context);

        if (actor.isPresent()) {
            ClientBO client = actor.get();
            boolean isAuthClient = Requester.isAuthClient(client);

            if (isAuthClient) {
                if (!Requester.authClientCanPerform(authRequest)) {
                    context.status(403)
                            .json(new Error("", "Auth clients can't set user agent or source IP of an auth request"));

                    return Optional.empty();
                }

                if (authRequest.getDomain() != null
                    && !ActorDomainVerifier.verifyAuthClientDomain(client, context, authRequest.getDomain())) {
                    return Optional.empty();
                }
            }

            if (authRequest.getSourceIp() == null) {
                authRequest = authRequest.withSourceIp(context.ip());
            }

            if (authRequest.getUserAgent() == null && context.userAgent() != null) {
                authRequest = authRequest.withUserAgent(context.userAgent());
            }

            return Optional.of(authRequest.withClientId("" + client.getId())); // TODO migrate that to long as well
        }

        return Optional.of(authRequest);
    }

    private Instant parseOffsetDateTime(final String str) {
        try {
            return str == null ? null : Instant.parse(str);
        } catch (final DateTimeParseException e) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("fromTimestamp", ViolationType.INVALID_VALUE)
            ));
        }
    }
}
