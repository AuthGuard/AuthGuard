package com.authguard.rest.routes;

import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import com.authguard.rest.dto.AuthRequestDTO;
import com.authguard.rest.dto.TokensDTO;
import com.authguard.service.AuthenticationService;

import java.util.Optional;

import static io.javalin.apibuilder.ApiBuilder.post;

public class AuthRoute implements EndpointGroup {
    private final AuthenticationService authenticationService;
    private final RestMapper restMapper;

    @Inject
    AuthRoute(final AuthenticationService authenticationService, final RestMapper restMapper) {
        this.authenticationService = authenticationService;
        this.restMapper = restMapper;
    }

    @Override
    public void addEndpoints() {
        post("/authenticate", this::authenticate);
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
}
