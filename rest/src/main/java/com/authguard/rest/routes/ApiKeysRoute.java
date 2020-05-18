package com.authguard.rest.routes;

import com.authguard.api.dto.TokensDTO;
import com.authguard.rest.access.ActorRoles;
import com.authguard.service.ApiKeysService;
import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.post;

public class ApiKeysRoute implements EndpointGroup {
    private final ApiKeysService apiKeysService;

    @Inject
    public ApiKeysRoute(final ApiKeysService apiKeysService, final RestMapper restMapper) {
        this.apiKeysService = apiKeysService;
    }

    @Override
    public void addEndpoints() {
        post("/:id", this::generate, ActorRoles.anyAdmin());
    }

    private void generate(final Context context) {
        final String appId = context.pathParam("id");

        final TokensDTO key = TokensDTO.builder()
                .token(apiKeysService.generateApiKey(appId))
                .build();

        context.status(201).json(key);
    }
}
