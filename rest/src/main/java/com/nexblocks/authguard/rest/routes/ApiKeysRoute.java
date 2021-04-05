package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.dto.entities.ApiKeyDTO;
import com.nexblocks.authguard.api.dto.entities.Error;
import com.nexblocks.authguard.api.dto.requests.ApiKeyRequestDTO;
import com.nexblocks.authguard.api.routes.ApiKeysApi;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.service.ApiKeysService;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.ApiKeyBO;
import io.javalin.http.Context;

import java.util.Optional;

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

    @Override
    public void getById(final Context context) {
        final String apiKeyId = context.pathParam("id");

        final Optional<ApiKeyDTO> apiKey = apiKeysService.getById(apiKeyId)
                .map(restMapper::toDTO);

        if (apiKey.isPresent()) {
            context.status(200).json(apiKey.get());
        } else {
            context.status(404)
                    .json(new Error(ErrorCode.API_KEY_DOES_NOT_EXIST.getCode(), "API key does not exist"));
        }
    }

    @Override
    public void deleteById(final Context context) {
        final String apiKeyId = context.pathParam("id");

        final Optional<ApiKeyDTO> apiKey = apiKeysService.delete(apiKeyId)
                .map(restMapper::toDTO);

        if (apiKey.isPresent()) {
            context.status(200).json(apiKey.get());
        } else {
            context.status(404)
                    .json(new Error(ErrorCode.API_KEY_DOES_NOT_EXIST.getCode(), "API key does not exist"));
        }
    }
}
