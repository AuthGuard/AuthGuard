package com.authguard.rest.routes;

import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import com.authguard.rest.dto.TokensDTO;
import com.authguard.service.ApiKeysService;

import static io.javalin.apibuilder.ApiBuilder.post;

public class ApiKeysRoute implements EndpointGroup {
    private final ApiKeysService apiKeysService;

    @Inject
    public ApiKeysRoute(final ApiKeysService apiKeysService, final RestMapper restMapper) {
        this.apiKeysService = apiKeysService;
    }

    @Override
    public void addEndpoints() {
        post("/:id", this::generate);
    }

    private void generate(final Context context) {
        final String appId = context.pathParam("id");

        final TokensDTO key = TokensDTO.builder()
                .token(apiKeysService.generateApiKey(appId))
                .build();

        context.json(key);
    }
}
