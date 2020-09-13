package com.authguard.rest.routes;

import com.authguard.api.dto.entities.TokensDTO;
import com.authguard.api.routes.ApiKeysApi;
import com.authguard.rest.mappers.RestMapper;
import com.authguard.service.ApiKeysService;
import com.google.inject.Inject;
import io.javalin.http.Context;

public class ApiKeysRoute extends ApiKeysApi {
    private final ApiKeysService apiKeysService;

    @Inject
    public ApiKeysRoute(final ApiKeysService apiKeysService, final RestMapper restMapper) {
        this.apiKeysService = apiKeysService;
    }

    public void generate(final Context context) {
        final String appId = context.pathParam("id");

        final TokensDTO key = TokensDTO.builder()
                .token(apiKeysService.generateApiKey(appId))
                .build();

        context.status(201).json(key);
    }
}
