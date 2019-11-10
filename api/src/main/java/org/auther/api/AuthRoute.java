package org.auther.api;

import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.auther.api.dto.AuthRequestDTO;
import org.auther.api.dto.TokensDTO;
import org.auther.service.AuthenticationService;
import org.auther.service.AuthorizationService;

import java.util.Optional;

import static io.javalin.apibuilder.ApiBuilder.post;

public class AuthRoute implements EndpointGroup {
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;
    private final RestMapper restMapper;

    @Inject
    AuthRoute(final AuthenticationService authenticationService, final AuthorizationService authorizationService,
              final RestMapper restMapper) {
        this.authenticationService = authenticationService;
        this.authorizationService = authorizationService;
        this.restMapper = restMapper;
    }

    @Override
    public void addEndpoints() {
        post("/authenticate", this::authenticate);
        post("/authorize", this::authorize);
    }

    private void authenticate(final Context context) {
        final AuthRequestDTO authenticationRequest = context.bodyAsClass(AuthRequestDTO.class);

        final Optional<TokensDTO> tokens = authenticationService.authenticate(authenticationRequest.getAuthorization())
                .map(restMapper::toDTO);

        if (tokens.isPresent()) {
            context.json(tokens.get());
        } else {
            context.status(400).result("Failed to authenticate user");
        }
    }

    private void authorize(final Context context) {
        final AuthRequestDTO authenticationRequest = context.bodyAsClass(AuthRequestDTO.class);

        final Optional<TokensDTO> tokens = authorizationService.authorize(authenticationRequest.getAuthorization())
                .map(restMapper::toDTO);

        if (tokens.isPresent()) {
            context.json(tokens.get());
        } else {
            context.status(400).result("Failed to authorize user");
        }
    }
}
