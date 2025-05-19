package com.nexblocks.authguard.rest.routes;

import com.nexblocks.authguard.api.access.ActorRoles;
import com.google.inject.Inject;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.dto.entities.AuthResponseDTO;
import com.nexblocks.authguard.api.dto.requests.OtpRequestDTO;
import com.nexblocks.authguard.api.dto.validation.IdParser;
import com.nexblocks.authguard.api.routes.OtpApi;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.api.common.BodyHandler;
import com.nexblocks.authguard.api.common.RequestContextExtractor;
import com.nexblocks.authguard.service.OtpService;
import com.nexblocks.authguard.service.model.RequestContextBO;
import io.javalin.http.Context;

import java.util.concurrent.CompletableFuture;

import static io.javalin.apibuilder.ApiBuilder.post;

@DependsOnConfiguration("otp")
public class OtpRoute extends OtpApi {
    private final OtpService otpService;
    private final RestMapper restMapper;
    private final BodyHandler<OtpRequestDTO> otpRequestBodyHandler;

    @Inject
    public OtpRoute(final OtpService otpService, final RestMapper restMapper) {
        this.otpService = otpService;
        this.restMapper = restMapper;
        this.otpRequestBodyHandler = new BodyHandler.Builder<>(OtpRequestDTO.class)
                .build();
    }

    @Override
    public String getPath() {
        return "/domains/{domain}/otp";
    }

    @Override
    public void addEndpoints() {
        post("/verify", this::verify, ActorRoles.adminOrAuthClient());
    }

    public void verify(final Context context) {
        OtpRequestDTO body = otpRequestBodyHandler.getValidated(context);
        RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        CompletableFuture<AuthResponseDTO> tokens = otpService.authenticate(IdParser.from(body.getPasswordId()), body.getPassword(), requestContext)
                .thenApply(restMapper::toDTO);

        context.future(() -> tokens.thenAccept(context::json));
    }
}
