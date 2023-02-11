package com.nexblocks.authguard.service.mappers;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.TokenOptionsBO;

public class TokenOptionsMapper {
    public static TokenOptionsBO fromAuthRequest(final AuthRequestBO request) {
        return TokenOptionsBO.builder()
                .deviceId(request.getDeviceId())
                .externalSessionId(request.getExternalSessionId())
                .sourceIp(request.getSourceIp())
                .userAgent(request.getUserAgent())
                .build();
    }
}
