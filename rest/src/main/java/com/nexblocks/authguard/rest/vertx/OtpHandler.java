package com.nexblocks.authguard.rest.vertx;

import com.google.inject.Inject;
import com.nexblocks.authguard.api.access.VertxRolesAccessHandler;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.api.common.RequestContextExtractor;
import com.nexblocks.authguard.api.dto.requests.OtpRequestDTO;
import com.nexblocks.authguard.api.dto.validation.IdParser;
import com.nexblocks.authguard.api.routes.VertxApiHandler;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.service.OtpService;
import com.nexblocks.authguard.service.model.RequestContextBO;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@DependsOnConfiguration("otp")
public class OtpHandler implements VertxApiHandler {
    private final OtpService otpService;
    private final RestMapper restMapper;
    private final BodyHandler<OtpRequestDTO> otpRequestBodyHandler;

    @Inject
    public OtpHandler(final OtpService otpService, final RestMapper restMapper) {
        this.otpService = otpService;
        this.restMapper = restMapper;
        this.otpRequestBodyHandler = new BodyHandler.Builder<>(OtpRequestDTO.class).build();
    }

    public void register(final Router router) {
        router.post("/domains/:domain/otp/verify")
                .handler(VertxRolesAccessHandler.adminOrAuthClient())
                .handler(this::verify);
    }

    private void verify(final RoutingContext context) {
        try {
            OtpRequestDTO body = otpRequestBodyHandler.getValidated(context);
            RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

            otpService.authenticate(IdParser.from(body.getPasswordId()), body.getPassword(), requestContext)
                    .map(restMapper::toDTO)
                    .subscribe().withSubscriber(new VertxJsonSubscriber<>(context));
        } catch (Exception e) {
            context.fail(e);
        }
    }
}

