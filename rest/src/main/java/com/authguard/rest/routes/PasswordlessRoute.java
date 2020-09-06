package com.authguard.rest.routes;

import com.authguard.api.dto.requests.PasswordlessRequestDTO;
import com.authguard.rest.access.ActorRoles;
import com.authguard.rest.util.BodyHandler;
import com.authguard.service.PasswordlessService;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.post;

public class PasswordlessRoute implements EndpointGroup {
    private final PasswordlessService passwordlessService;
    private final BodyHandler<PasswordlessRequestDTO> passwordlessRequestBodyHandler;

    @Inject
    public PasswordlessRoute(final PasswordlessService passwordlessService) {
        this.passwordlessService = passwordlessService;
        this.passwordlessRequestBodyHandler = new BodyHandler.Builder<>(PasswordlessRequestDTO.class)
                .build();
    }

    @Override
    public void addEndpoints() {
        post("/verify", this::verify, ActorRoles.adminClient());
    }

    private void verify(final Context context) {
        final PasswordlessRequestDTO request = passwordlessRequestBodyHandler.getValidated(context);

        final TokensBO generatedTokens = passwordlessService.authenticate(request.getToken());

        context.json(generatedTokens);
    }
}
