package com.nexblocks.authguard.rest.routes;

import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.dto.entities.TokensDTO;
import com.nexblocks.authguard.api.dto.requests.OtpRequestDTO;
import com.nexblocks.authguard.api.routes.OtpApi;
import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.util.BodyHandler;
import com.nexblocks.authguard.rest.util.RequestContextExtractor;
import com.nexblocks.authguard.service.OtpService;
import com.google.inject.Inject;
import com.nexblocks.authguard.service.model.RequestContextBO;
import io.javalin.http.Context;

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

    public void verify(final Context context) {
        final OtpRequestDTO body = otpRequestBodyHandler.getValidated(context);
        final RequestContextBO requestContext = RequestContextExtractor.extractWithoutIdempotentKey(context);

        final TokensDTO tokens = restMapper.toDTO(otpService.authenticate(body.getPasswordId(), body.getPassword(), requestContext));

        context.json(tokens);
    }
}
