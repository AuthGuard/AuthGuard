package com.nexblocks.authguard.rest.routes;

import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.dto.requests.PasswordlessRequestDTO;
import com.nexblocks.authguard.api.routes.PasswordlessApi;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.rest.util.RequestContextExtractor;
import com.nexblocks.authguard.service.PasswordlessService;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import com.google.inject.Inject;
import io.javalin.http.Context;

@DependsOnConfiguration("passwordless")
public class PasswordlessRoute extends PasswordlessApi {
    private final PasswordlessService passwordlessService;
    private final BodyHandler<PasswordlessRequestDTO> passwordlessRequestBodyHandler;

    @Inject
    public PasswordlessRoute(final PasswordlessService passwordlessService) {
        this.passwordlessService = passwordlessService;
        this.passwordlessRequestBodyHandler = new BodyHandler.Builder<>(PasswordlessRequestDTO.class)
                .build();
    }

    public void verify(final Context context) {
        final PasswordlessRequestDTO request = passwordlessRequestBodyHandler.getValidated(context);
        final RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        final AuthResponseBO generatedTokens = passwordlessService.authenticate(request.getToken(), requestContext);

        context.json(generatedTokens);
    }
}
