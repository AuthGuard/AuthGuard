package com.authguard.rest.routes;

import com.authguard.api.dto.entities.TokensDTO;
import com.authguard.api.dto.requests.AuthRequestDTO;
import com.authguard.api.routes.AuthApi;
import com.authguard.rest.mappers.RestMapper;
import com.authguard.rest.util.BodyHandler;
import com.authguard.service.AuthenticationService;
import com.authguard.service.ExchangeService;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import io.javalin.http.Context;

import java.util.Optional;

public class AuthRoute extends AuthApi {
    private final AuthenticationService authenticationService;
    private final ExchangeService exchangeService;
    private final RestMapper restMapper;

    private final BodyHandler<AuthRequestDTO> authRequestBodyHandler;

    @Inject
    AuthRoute(final AuthenticationService authenticationService, final ExchangeService exchangeService,
              final RestMapper restMapper) {
        this.authenticationService = authenticationService;
        this.exchangeService = exchangeService;
        this.restMapper = restMapper;

        this.authRequestBodyHandler = new BodyHandler.Builder<>(AuthRequestDTO.class)
                .build();
    }

    public void authenticate(final Context context) {
        final AuthRequestDTO authenticationRequest = authRequestBodyHandler.getValidated(context);

        final Optional<TokensDTO> tokens = authenticationService.authenticate(authenticationRequest.getAuthorization())
                .map(restMapper::toDTO);

        if (tokens.isPresent()) {
            context.json(tokens.get());
        } else {
            context.status(400).result("Failed to authenticate user");
        }
    }

    public void exchange(final Context context) {
        final AuthRequestDTO authenticationRequest = authRequestBodyHandler.getValidated(context);
        final String from = context.queryParam("from");
        final String to = context.queryParam("to");

        final TokensBO tokens;

        if (authenticationRequest.getRestrictions() == null) {
            tokens = exchangeService.exchange(authenticationRequest.getAuthorization(), from, to);
        } else {
            tokens = exchangeService.exchange(authenticationRequest.getAuthorization(),
                    restMapper.toBO(authenticationRequest.getRestrictions()), from, to);
        }

        context.json(restMapper.toDTO(tokens));
    }
}
