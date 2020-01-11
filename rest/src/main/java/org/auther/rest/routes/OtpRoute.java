package org.auther.rest.routes;

import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.auther.rest.dto.OtpRequestDTO;
import org.auther.rest.dto.TokensDTO;
import org.auther.service.OtpService;

import static io.javalin.apibuilder.ApiBuilder.post;

public class OtpRoute implements EndpointGroup {
    private final OtpService otpService;
    private final RestMapper restMapper;

    @Inject
    public OtpRoute(final OtpService otpService, final RestMapper restMapper) {
        this.otpService = otpService;
        this.restMapper = restMapper;
    }

    @Override
    public void addEndpoints() {
        post("/verify", this::verify);
    }

    private void verify(final Context context) {
        final OtpRequestDTO body = context.bodyAsClass(OtpRequestDTO.class);

        final TokensDTO tokens = restMapper.toDTO(otpService.authenticate(body.getPasswordId(), body.getPassword()));

        context.json(tokens);
    }
}
