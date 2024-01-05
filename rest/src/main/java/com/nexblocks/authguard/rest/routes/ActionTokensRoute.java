package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.dto.entities.ActionTokenDTO;
import com.nexblocks.authguard.api.dto.entities.ActionTokenRequestType;
import com.nexblocks.authguard.api.dto.entities.AuthResponseDTO;
import com.nexblocks.authguard.api.dto.requests.ActionTokenRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.ActionTokensApi;
import com.nexblocks.authguard.rest.exceptions.RequestValidationException;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.service.ActionTokenService;
import com.nexblocks.authguard.service.model.ActionTokenBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import io.javalin.core.validation.Validator;
import io.javalin.http.Context;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@DependsOnConfiguration("otp")
public class ActionTokensRoute extends ActionTokensApi {
    private final ActionTokenService actionTokenService;
    private final RestMapper restMapper;

    private final BodyHandler<ActionTokenRequestDTO> actionTokenRequestBodyHandler;

    @Inject
    public ActionTokensRoute(final ActionTokenService actionTokenService, final RestMapper restMapper) {
        this.actionTokenService = actionTokenService;
        this.restMapper = restMapper;

        this.actionTokenRequestBodyHandler = new BodyHandler.Builder<>(ActionTokenRequestDTO.class)
                .build();
    }

    @Override
    public void createOtp(final Context context) {
        Validator<Long> accountId = context.pathParam("id", Long.class);

        if (!accountId.isValid()) {
            throw new RequestValidationException(Collections.singletonList(new Violation("id", ViolationType.INVALID_VALUE)));
        }

        if (accountId == null) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("accountId", ViolationType.MISSING_REQUIRED_VALUE)
            ));
        }

        CompletableFuture<AuthResponseDTO> result = actionTokenService.generateOtp(accountId.get())
                .thenApply(restMapper::toDTO);

        context.status(201).json(result);
    }

    @Override
    public void createToken(final Context context) {
        ActionTokenRequestDTO request = actionTokenRequestBodyHandler.getValidated(context);

        CompletableFuture<ActionTokenBO> result;

        if (request.getType() == ActionTokenRequestType.OTP) {
            result = actionTokenService.generateFromOtp(request.getOtp().getPasswordId(),
                    request.getOtp().getPassword(), request.getAction());
        } else {
            AuthRequestBO authRequest = restMapper.toBO(request.getBasic());
            result = actionTokenService.generateFromBasicAuth(authRequest, request.getAction());
        }

        context.status(201).json(result.thenApply(restMapper::toDTO));
    }

    @Override
    public void verifyToken(final Context context) {
        String token = context.queryParam("token");
        String action = context.queryParam("action");

        if (token == null) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("token", ViolationType.MISSING_REQUIRED_VALUE)
            ));
        }

        if (action == null) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("action", ViolationType.MISSING_REQUIRED_VALUE)
            ));
        }

        CompletableFuture<ActionTokenDTO> result = actionTokenService.verifyToken(token, action)
                .thenApply(restMapper::toDTO);


        context.status(200).json(result);
    }
}
