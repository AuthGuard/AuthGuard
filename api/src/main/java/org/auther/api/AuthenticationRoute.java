package org.auther.api;

import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.auther.api.dto.AuthenticationRequestDTO;
import org.auther.api.dto.TokensDTO;
import org.auther.service.AuthenticationService;

import java.util.Optional;

import static io.javalin.apibuilder.ApiBuilder.post;

public class AuthenticationRoute implements EndpointGroup {
    private final AuthenticationService authenticationService;
    private final RestMapper restMapper;

    @Inject
    AuthenticationRoute(final AuthenticationService authenticationService, final RestMapper restMapper) {
        this.authenticationService = authenticationService;
        this.restMapper = restMapper;
    }

    @Override
    public void addEndpoints() {
        post("/authenticate", this::authenticate);
    }

    private void authenticate(final Context context) {
        final AuthenticationRequestDTO authenticationRequest = context.bodyAsClass(AuthenticationRequestDTO.class);

        final Optional<TokensDTO> tokens = authenticationService.authenticate(authenticationRequest.getAuthorization())
                .map(restMapper::toDTO);

        if (tokens.isPresent()) {
            context.json(tokens.get());
        } else {
            context.status(400).result("Failed to authenticate user");
        }
    }
}
