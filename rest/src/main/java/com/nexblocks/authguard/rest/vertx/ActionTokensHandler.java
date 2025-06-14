package com.nexblocks.authguard.rest.vertx;

import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.api.common.Domain;
import com.nexblocks.authguard.api.common.RequestValidationException;
import com.nexblocks.authguard.api.dto.entities.ActionTokenDTO;
import com.nexblocks.authguard.api.dto.entities.ActionTokenRequestType;
import com.nexblocks.authguard.api.dto.entities.AuthResponseDTO;
import com.nexblocks.authguard.api.dto.requests.ActionTokenRequestDTO;
import com.nexblocks.authguard.api.dto.validation.IdParser;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.api.dto.validation.violations.ViolationType;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.ActionTokenService;
import com.nexblocks.authguard.service.model.ActionTokenBO;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import javax.inject.Inject;
import java.util.Collections;

public class ActionTokensHandler implements VertxApiHandler {
    private final ActionTokenService actionTokenService;
    private final RestMapper restMapper;
    private final BodyHandler<ActionTokenRequestDTO> actionTokenRequestBodyHandler;

    @Inject
    public ActionTokensHandler(final ActionTokenService actionTokenService, final RestMapper restMapper) {
        this.actionTokenService = actionTokenService;
        this.restMapper = restMapper;
        this.actionTokenRequestBodyHandler = new BodyHandler.Builder<>(ActionTokenRequestDTO.class).build();
    }

    public void register(final Router router) {
        router.post("/domains/:domain/actions/otp").handler(this::createOtp);
        router.post("/domains/:domain/actions/token").handler(this::createToken);
        router.post("/domains/:domain/actions/verify").handler(this::verifyToken);
    }

    private void createOtp(final RoutingContext context) {
        final String accountIdStr = context.pathParam("id");

        if (accountIdStr == null) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("accountId", ViolationType.MISSING_REQUIRED_VALUE)
            ));
        }

        final long accountId;
        try {
            accountId = Long.parseLong(accountIdStr);
        } catch (NumberFormatException e) {
            throw new RequestValidationException(Collections.singletonList(
                    new Violation("id", ViolationType.INVALID_VALUE)
            ));
        }

        Uni<AuthResponseDTO> result = actionTokenService.generateOtp(accountId, Domain.fromContext(context))
                .map(restMapper::toDTO);

        result.subscribe().withSubscriber(new VertxJsonSubscriber<>(context, 201));
    }

    private void createToken(final RoutingContext context) {
        ActionTokenRequestDTO request = actionTokenRequestBodyHandler.getValidated(context);

        Uni<ActionTokenBO> result;

        if (request.getType() == ActionTokenRequestType.OTP) {
            result = actionTokenService.generateFromOtp(
                    IdParser.from(request.getOtp().getPasswordId()),
                    Domain.fromContext(context),
                    request.getOtp().getPassword(),
                    request.getAction()
            );
        } else {
            AuthRequestBO authRequest = restMapper.toBO(request.getBasic());
            result = actionTokenService.generateFromBasicAuth(authRequest, request.getAction());
        }

        result.map(restMapper::toDTO).subscribe().withSubscriber(new VertxJsonSubscriber<>(context, 201));
    }

    private void verifyToken(final RoutingContext context) {
        final String token = context.queryParam("token").get(0); // TODO use Optional
        final String action = context.queryParam("action").get(0); // TODO use Optional

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

        Uni<ActionTokenDTO> result = actionTokenService.verifyToken(token, action)
                .map(restMapper::toDTO);

        result.subscribe().withSubscriber(new VertxJsonSubscriber<>(context));
    }
}

