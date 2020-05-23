package com.authguard.rest.routes;

import com.authguard.api.dto.AuthRequestDTO;
import com.authguard.api.dto.TokensDTO;
import com.authguard.rest.access.ActorRoles;
import com.authguard.service.ExchangeService;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import com.authguard.service.AuthenticationService;

import java.util.Optional;

import static io.javalin.apibuilder.ApiBuilder.post;

public class AuthRoute implements EndpointGroup {
    private final AuthenticationService authenticationService;
    private final ExchangeService exchangeService;
    private final RestMapper restMapper;

    @Inject
    AuthRoute(final AuthenticationService authenticationService, final ExchangeService exchangeService,
              final RestMapper restMapper) {
        this.authenticationService = authenticationService;
        this.exchangeService = exchangeService;
        this.restMapper = restMapper;
    }

    @Override
    public void addEndpoints() {
        post("/authenticate", this::authenticate, ActorRoles.adminClient());
        post("/exchange", this::exchange, ActorRoles.adminClient());
    }

    private void authenticate(final Context context) {
        final AuthRequestDTO authenticationRequest = RestJsonMapper.asClass(context.body(), AuthRequestDTO.class);

        final Optional<TokensDTO> tokens = authenticationService.authenticate(authenticationRequest.getAuthorization())
                .map(restMapper::toDTO);

        if (tokens.isPresent()) {
            context.json(tokens.get());
        } else {
            context.status(400).result("Failed to authenticate user");
        }
    }

    private void exchange(final Context context) {
        final AuthRequestDTO authenticationRequest = RestJsonMapper.asClass(context.body(), AuthRequestDTO.class);
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
