package com.authguard.rest.routes;

import com.authguard.rest.dto.PasswordlessRequestDTO;
import com.authguard.service.PasswordlessService;
import com.authguard.service.model.TokensBO;
import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.post;

public class PasswordlessRoute implements EndpointGroup {
    private final PasswordlessService passwordlessService;

    @Inject
    public PasswordlessRoute(final PasswordlessService passwordlessService) {
        this.passwordlessService = passwordlessService;
    }

    @Override
    public void addEndpoints() {
        post("/verify", this::verify);
    }

    private void verify(final Context context) {
        final PasswordlessRequestDTO request = RestJsonMapper.asClass(context.body(), PasswordlessRequestDTO.class);

        final TokensBO generatedTokens = passwordlessService.authenticate(request.getToken());

        context.json(generatedTokens);
    }
}
