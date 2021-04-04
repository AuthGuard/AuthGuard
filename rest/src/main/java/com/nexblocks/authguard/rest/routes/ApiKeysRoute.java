package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.dto.requests.ApiKeyRequestDTO;
import com.nexblocks.authguard.api.routes.ApiKeysApi;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.model.ApiKeyBO;
import io.javalin.http.Context;

public class ApiKeysRoute extends ApiKeysApi {
    private final ApiKeysService apiKeysService;
    private final RestMapper restMapper;

    private final BodyHandler<ApiKeyRequestDTO> apiKeyRequestBodyHandler;

    @Inject
    public ApiKeysRoute(final ApiKeysService apiKeysService, final RestMapper restMapper) {
        this.apiKeysService = apiKeysService;
        this.restMapper = restMapper;

        this.apiKeyRequestBodyHandler = new BodyHandler.Builder<>(ApiKeyRequestDTO.class)
                .build();
    }

    public void generate(final Context context) {
        final ApiKeyRequestDTO request = apiKeyRequestBodyHandler.getValidated(context);

        final ApiKeyBO key = apiKeysService.generateApiKey(request.getAppId());

        context.status(201).json(restMapper.toDTO(key));
    }
}
