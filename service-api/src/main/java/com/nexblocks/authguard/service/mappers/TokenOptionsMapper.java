package com.nexblocks.authguard.service.mappers;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;

public class TokenOptionsMapper {
    public static TokenOptionsBO.Builder fromAuthRequest(final AuthRequestBO request) {
        return TokenOptionsBO.builder()
                .clientId(request.getClientId())
                .deviceId(request.getDeviceId())
                .externalSessionId(request.getExternalSessionId())
                .sourceIp(request.getSourceIp())
                .userAgent(request.getUserAgent());
    }
}
