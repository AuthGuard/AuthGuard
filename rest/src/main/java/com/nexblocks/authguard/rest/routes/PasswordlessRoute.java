package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.dto.entities.AuthResponseDTO;
import com.nexblocks.authguard.api.dto.requests.PasswordlessRequestDTO;
import com.nexblocks.authguard.api.routes.PasswordlessApi;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.rest.util.RequestContextExtractor;
import com.nexblocks.authguard.service.PasswordlessService;
import com.nexblocks.authguard.service.model.RequestContextBO;
import io.javalin.http.Context;

import java.util.concurrent.CompletableFuture;

@DependsOnConfiguration("passwordless")
public class PasswordlessRoute extends PasswordlessApi {
    private final PasswordlessService passwordlessService;
    private final RestMapper restMapper;
    private final BodyHandler<PasswordlessRequestDTO> passwordlessRequestBodyHandler;

    @Inject
    public PasswordlessRoute(final PasswordlessService passwordlessService, final RestMapper restMapper) {
        this.passwordlessService = passwordlessService;
        this.restMapper = restMapper;
        this.passwordlessRequestBodyHandler = new BodyHandler.Builder<>(PasswordlessRequestDTO.class)
                .build();
    }

    public void verify(final Context context) {
        PasswordlessRequestDTO request = passwordlessRequestBodyHandler.getValidated(context);
        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        CompletableFuture<AuthResponseDTO> generatedTokens = passwordlessService.authenticate(request.getToken(), requestContext)
                .thenApply(restMapper::toDTO);

        context.json(generatedTokens);
    }
}
