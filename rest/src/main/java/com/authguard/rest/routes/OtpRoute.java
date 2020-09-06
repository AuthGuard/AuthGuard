package com.authguard.rest.routes;

import com.authguard.api.dto.requests.OtpRequestDTO;
import com.authguard.api.dto.entities.TokensDTO;
import com.authguard.rest.access.ActorRoles;
import com.authguard.rest.util.BodyHandler;
import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import com.authguard.service.OtpService;

import static io.javalin.apibuilder.ApiBuilder.post;

public class OtpRoute implements EndpointGroup {
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
    public void addEndpoints() {
        post("/verify", this::verify, ActorRoles.adminClient());
    }

    private void verify(final Context context) {
        final OtpRequestDTO body = otpRequestBodyHandler.getValidated(context);

        final TokensDTO tokens = restMapper.toDTO(otpService.authenticate(body.getPasswordId(), body.getPassword()));

        context.json(tokens);
    }
}
