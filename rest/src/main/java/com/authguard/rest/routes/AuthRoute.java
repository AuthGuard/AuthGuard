package com.authguard.rest.routes;

import com.authguard.api.dto.entities.Error;
import com.authguard.api.dto.entities.ExchangeAttemptDTO;
import com.authguard.api.dto.entities.TokensDTO;
import com.authguard.api.dto.requests.AuthRequestDTO;
import com.authguard.api.dto.validation.violations.Violation;
import com.authguard.api.dto.validation.violations.ViolationType;
import com.authguard.api.routes.AuthApi;
import com.authguard.rest.exceptions.RequestValidationException;
import com.authguard.rest.mappers.RestMapper;
import com.authguard.rest.util.BodyHandler;
import com.authguard.service.AuthenticationService;
import com.authguard.service.ExchangeAttemptsService;
import com.authguard.service.ExchangeService;
import com.authguard.service.exceptions.codes.ErrorCode;
import com.authguard.service.model.ExchangeAttemptsQueryBO;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import io.javalin.http.Context;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public void authenticate(final Context context) {
        final AuthRequestDTO authenticationRequest = authRequestBodyHandler.getValidated(context);

        final Optional<TokensDTO> tokens = authenticationService.authenticate(restMapper.toBO(authenticationRequest))
                .map(restMapper::toDTO);

        if (tokens.isPresent()) {
            context.json(tokens.get());
        } else {
            context.status(400).json(new Error("400", "Failed to authenticate user"));
        }
    }

    public void exchange(final Context context) {
        final AuthRequestDTO authenticationRequest = authRequestBodyHandler.getValidated(context);
        final String from = context.queryParam("from");
        final String to = context.queryParam("to");

        final TokensBO tokens;

        if (authenticationRequest.getRestrictions() == null) {
            tokens = exchangeService.exchange(restMapper.toBO(authenticationRequest), from, to);
        } else {
            tokens = exchangeService.exchange(restMapper.toBO(authenticationRequest),
                    restMapper.toBO(authenticationRequest.getRestrictions()), from, to);
        }

        context.json(restMapper.toDTO(tokens));
    }

    @Override
    public void getExchangeAttempts(final Context context) {
        final String entityId = context.queryParam("entityId");
        final OffsetDateTime fromTimestamp = parseOffsetDateTime(context.queryParam("fromTimestamp"));
        final String fromExchange = context.queryParam("fromExchange");

        // take care of checking the parameters
        if (entityId == null) {
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
        final ExchangeAttemptsQueryBO query = ExchangeAttemptsQueryBO.builder()
                .entityId(entityId)
                .fromTimestamp(fromTimestamp)
                .fromExchange(fromExchange)
                .build();

        final Collection<ExchangeAttemptDTO> attempts = exchangeAttemptsService.find(query)
                .stream()
                .map(restMapper::toDTO)
                .collect(Collectors.toList());

        context.json(attempts);
    }

    private OffsetDateTime parseOffsetDateTime(final String str) {
        try {
            return str == null ? null : OffsetDateTime.parse(str);
        } catch (final DateTimeParseException e) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("fromTimestamp", ViolationType.INVALID_VALUE)
            ));
        }
    }
}
