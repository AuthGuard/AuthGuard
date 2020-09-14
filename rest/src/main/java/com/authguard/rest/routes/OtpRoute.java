package com.authguard.rest.routes;

import com.authguard.api.dto.entities.TokensDTO;
import com.authguard.api.dto.requests.OtpRequestDTO;
import com.authguard.api.routes.OtpApi;
import com.authguard.rest.mappers.RestMapper;
import com.authguard.rest.util.BodyHandler;
import com.authguard.service.OtpService;
import com.google.inject.Inject;
import io.javalin.http.Context;

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

    public void verify(final Context context) {
        final OtpRequestDTO body = otpRequestBodyHandler.getValidated(context);

        final TokensDTO tokens = restMapper.toDTO(otpService.authenticate(body.getPasswordId(), body.getPassword()));

        context.json(tokens);
    }
}
