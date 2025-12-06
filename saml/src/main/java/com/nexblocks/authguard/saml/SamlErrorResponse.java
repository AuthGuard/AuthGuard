package com.nexblocks.authguard.saml;

import com.nexblocks.authguard.api.dto.entities.RequestValidationError;
import org.opensaml.saml.saml2.core.Response;

public class SamlErrorResponse {
    private final boolean canReturnToSp;
    private final RequestValidationError error;
    private final Response response;
    private final String acsUrl;

    private SamlErrorResponse(final boolean canReturnToSp, final RequestValidationError error, final Response response, final String acsUrl) {
        this.canReturnToSp = canReturnToSp;
        this.error = error;
        this.response = response;
        this.acsUrl = acsUrl;
    }

    public static SamlErrorResponse serviceProviderError(final Response response, final String acsUrl) {
        return new SamlErrorResponse(true, null, response, acsUrl);
    }

    // this is for errors where we can't identify the service provider so we just display our own error message
    public static SamlErrorResponse nonServiceProviderError(final RequestValidationError error) {
        return new SamlErrorResponse(true, error, null, null);
    }

    public boolean isCanReturnToSp() {
        return canReturnToSp;
    }

    public RequestValidationError getError() {
        return error;
    }

    public Response getResponse() {
        return response;
    }

    public String getAcsUrl() {
        return acsUrl;
    }
}
