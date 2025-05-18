package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.VertxRolesAccessHandler;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.api.common.RequestContextExtractor;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.entities.ExchangeAttemptDTO;
import com.nexblocks.authguard.api.dto.requests.AuthRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.rest.access.ActorDomainVerifier;
import com.nexblocks.authguard.rest.access.Requester;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.AuthenticationService;
import com.nexblocks.authguard.service.ExchangeAttemptsService;
import com.nexblocks.authguard.service.ExchangeService;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.ClientBO;
import com.nexblocks.authguard.service.model.ExchangeAttemptsQueryBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AuthHandler implements VertxApiHandler {
    private final AuthenticationService authenticationService;
    private final ExchangeService exchangeService;
    private final ExchangeAttemptsService exchangeAttemptsService;
    private final RestMapper restMapper;
    private final BodyHandler<AuthRequestDTO> authRequestBodyHandler;

    @Inject
    public AuthHandler(final AuthenticationService authenticationService,
                          final ExchangeService exchangeService,
                          final ExchangeAttemptsService exchangeAttemptsService,
                          final RestMapper restMapper) {
        this.authenticationService = authenticationService;
        this.exchangeService = exchangeService;
        this.exchangeAttemptsService = exchangeAttemptsService;
        this.restMapper = restMapper;
        this.authRequestBodyHandler = new BodyHandler.Builder<>(AuthRequestDTO.class).build();
    }

    public void register(final Router router) {
        router.post("/domains/:domain/auth/authenticate")
                .handler(VertxRolesAccessHandler.adminOrAuthClient()).handler(this::authenticate);
        router.post("/domains/:domain/auth/logout")
                .handler(VertxRolesAccessHandler.adminOrAuthClient()).handler(this::logout);
        router.post("/domains/:domain/auth/refresh")
                .handler(VertxRolesAccessHandler.adminOrAuthClient()).handler(this::refresh);
        router.post("/domains/:domain/auth/exchange")
                .handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::exchange);
        router.post("/domains/:domain/auth/exchange/clear")
                .handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::clearToken);
        router.get("/domains/:domain/auth/exchange/attempts")
                .handler(VertxRolesAccessHandler.onlyAdminClient()).handler(this::getExchangeAttempts);
    }

    private void authenticate(final RoutingContext context) {
        Optional<AuthRequestDTO> authRequest = getValidRequestOrFail(context);

        if (authRequest.isEmpty()) {
            return;
        }

        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);
        AuthRequestBO bo = restMapper.toBO(authRequest.get());

        authenticationService.authenticate(bo, requestContext)
                .thenApply(restMapper::toDTO)
                .whenComplete((res, ex) -> {
                    if (ex != null) context.fail(ex);
                    else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                });
    }

    private void logout(final RoutingContext context) {
        AuthRequestDTO authRequest = authRequestBodyHandler.getValidated(context);
        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        authenticationService.logout(restMapper.toBO(authRequest), requestContext)
                .whenComplete((res, ex) -> {
                    if (ex != null) context.fail(ex);
                    else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                });
    }

    private void refresh(final RoutingContext context) {
        Optional<AuthRequestDTO> authRequest = getValidRequestOrFail(context);

        if (authRequest.isEmpty()) {
            return;
        }

        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        authenticationService.refresh(restMapper.toBO(authRequest.get()), requestContext)
                .thenApply(restMapper::toDTO)
                .whenComplete((res, ex) -> {
                    if (ex != null) context.fail(ex);
                    else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                });
    }

    private void exchange(final RoutingContext context) {
        Optional<AuthRequestDTO> authRequest = getValidRequestOrFail(context);

        if (authRequest.isEmpty()) {
            return;
        }

        String from = context.queryParam("from").get(0);
        String to = context.queryParam("to").get(0);

        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        exchangeService.exchange(restMapper.toBO(authRequest.get()), from, to, requestContext)
                .thenApply(restMapper::toDTO)
                .whenComplete((res, ex) -> {
                    if (ex != null) context.fail(ex);
                    else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                });
    }

    private void clearToken(final RoutingContext context) {
        AuthRequestDTO authRequest = authRequestBodyHandler.getValidated(context);
        String tokenType = context.queryParam("tokenType").get(0);

        if (tokenType == null) {
            context.response().setStatusCode(400).end(Json.encode(new Error("400", "Missing 'tokenType' query parameter")));
        } else {
            exchangeService.delete(restMapper.toBO(authRequest), tokenType)
                    .thenApply(restMapper::toDTO)
                    .whenComplete((res, ex) -> {
                        if (ex != null) context.fail(ex);
                        else context.response().putHeader("Content-Type", "application/json").end(Json.encode(res));
                    });
        }
    }

    private void getExchangeAttempts(final RoutingContext context) {
        try {
            Long entityId = context.queryParam("entityId") != null ?
                    Long.valueOf(context.queryParam("entityId").get(0)) : null;
            Instant fromTimestamp = parseOffsetDateTime(context.queryParam("fromTimestamp").get(0));
            String fromExchange = context.queryParam("fromExchange").get(0);

            if (entityId == null) {
                context.response().setStatusCode(400).end(Json.encode(
                        new Error(ErrorCode.MISSING_REQUEST_QUERY.getCode(), "Query parameter entityId is required")));
                return;
            }

            if (fromExchange != null && fromTimestamp == null) {
                context.response().setStatusCode(400).end(Json.encode(
                        new Error(ErrorCode.MISSING_REQUEST_QUERY.getCode(), "Query parameter fromTimestamp is required when fromExchange is set")));
                return;
            }

            ExchangeAttemptsQueryBO query = ExchangeAttemptsQueryBO.builder()
                    .entityId(entityId)
                    .fromTimestamp(fromTimestamp)
                    .fromExchange(fromExchange)
                    .build();

            List<ExchangeAttemptDTO> attempts = exchangeAttemptsService.find(query).stream()
                    .map(restMapper::toDTO)
                    .collect(Collectors.toList());

            context.response().putHeader("Content-Type", "application/json").end(Json.encode(attempts));
        } catch (Exception e) {
            context.fail(e);
        }
    }

    private Optional<AuthRequestDTO> getValidRequestOrFail(final RoutingContext context) {
        AuthRequestDTO authRequest = authRequestBodyHandler.getValidated(context);

        Optional<ClientBO> actor = Requester.getIfApp(context);

        if (actor.isPresent()) {
            ClientBO client = actor.get();
            boolean isAuthClient = Requester.isAuthClient(client);

            if (isAuthClient) {
                if (!Requester.authClientCanPerform(authRequest)) {
                    context.response().setStatusCode(403)
                            .end(Json.encode(new Error("", "Auth clients can't set user agent or source IP of an auth request")));
                    return Optional.empty();
                }

                if (authRequest.getDomain() != null
                        && !ActorDomainVerifier.verifyAuthClientDomain(client, context, authRequest.getDomain())) {
                    return Optional.empty();
                }
            }

            if (authRequest.getSourceIp() == null) {
                authRequest = authRequest.withSourceIp(context.request().remoteAddress().host());
            }

            if (authRequest.getUserAgent() == null && context.request().getHeader("User-Agent") != null) {
                authRequest = authRequest.withUserAgent(context.request().getHeader("User-Agent"));
            }

            return Optional.of(authRequest.withClientId(String.valueOf(client.getId())));
        }

        return Optional.of(authRequest);
    }

    private Instant parseOffsetDateTime(final String str) {
        try {
            return str == null ? null : Instant.parse(str);
        } catch (DateTimeParseException e) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("fromTimestamp", ViolationType.INVALID_VALUE)));
        }
    }
}
