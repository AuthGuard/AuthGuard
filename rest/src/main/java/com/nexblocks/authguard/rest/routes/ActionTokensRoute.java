package com.nexblocks.authguard.rest.routes;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.dto.entities.ActionTokenRequestType;
import com.nexblocks.authguard.api.dto.requests.ActionTokenRequestDTO;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.ActionTokensApi;
import com.nexblocks.authguard.rest.exceptions.RequestValidationException;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.service.ActionTokenService;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.model.ActionTokenBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;
import io.javalin.http.Context;
import io.vavr.control.Try;

import java.util.Collections;

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
        final String accountId = context.queryParam("account");

        if (accountId == null) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("accountId", ViolationType.MISSING_REQUIRED_VALUE)
            ));
        }

        final Try<AuthResponseBO> result = actionTokenService.generateOtp(accountId);

        if (result.isFailure()) {
            throw (ServiceException) result.getCause();
        }

        context.status(201).json(restMapper.toDTO(result.get()));
    }

    @Override
    public void createToken(final Context context) {
        final ActionTokenRequestDTO request = actionTokenRequestBodyHandler.getValidated(context);

        final Try<ActionTokenBO> result;

        if (request.getType() == ActionTokenRequestType.OTP) {
            result = actionTokenService.generateFromOtp(request.getOtp().getPasswordId(),
                    request.getOtp().getPassword(), request.getAction());
        } else {
            final AuthRequestBO authRequest = restMapper.toBO(request.getBasic());
            result = actionTokenService.generateFromBasicAuth(authRequest, request.getAction());
        }

        if (result.isFailure()) {
            throw (ServiceException) result.getCause();
        }

        context.status(201).json(restMapper.toDTO(result.get()));
    }

    @Override
    public void verifyToken(final Context context) {
        final String token = context.queryParam("token");
        final String action = context.queryParam("action");

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

        final Try<ActionTokenBO> result = actionTokenService.verifyToken(token, action);

        if (result.isFailure()) {
            throw (ServiceException) result.getCause();
        }

        context.status(200).json(restMapper.toDTO(result.get()));
    }
}
