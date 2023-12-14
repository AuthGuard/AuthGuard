package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.AuthRequestBO;
import com.nexblocks.authguard.service.model.RequestContextBO;
import com.nexblocks.authguard.service.model.AuthResponseBO;

public interface OtpService {
    AuthResponseBO authenticate(AuthRequestBO authRequest, RequestContextBO requestContext);

    AuthResponseBO authenticate(long passwordId, String otp, RequestContextBO requestContext);
}
