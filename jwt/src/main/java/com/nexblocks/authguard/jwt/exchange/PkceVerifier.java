package com.nexblocks.authguard.jwt.exchange;

import com.nexblocks.authguard.dal.model.AccountTokenDO;
import com.nexblocks.authguard.jwt.crypto.CryptoHash;
import com.nexblocks.authguard.service.exceptions.ServiceException;
import com.nexblocks.authguard.service.exceptions.codes.ErrorCode;
import com.nexblocks.authguard.service.model.AuthRequestBO;
import io.vavr.control.Try;

import java.util.Objects;

public class PkceVerifier {
    public static Try<Boolean> verifyIfPkce(AccountTokenDO accountToken, AuthRequestBO request) {
        String codeChallenge = accountToken.getAdditionalInformation() == null
                ? null
                : accountToken.getAdditionalInformation().get("codeChallenge");
        String codeChallengeMethod = accountToken.getAdditionalInformation() == null
                ? null
                : accountToken.getAdditionalInformation().get("codeChallengeMethod");

        boolean isRequestPkce = request.getExtraParameters() != null
                && PkceParameters.class.isAssignableFrom(request.getExtraParameters().getClass());

        boolean isAccountTokenPkce = codeChallenge != null || codeChallengeMethod != null;

        if (isRequestPkce != isAccountTokenPkce) {
            return Try.failure(new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "Request and stored token methods don't match (one uses PKCE and the other doesn't)"));
        }

        if (!isAccountTokenPkce) {
            return Try.success(true);
        }

        if (codeChallenge == null || codeChallengeMethod == null) {
            return Try.failure(new IllegalStateException("An invalid account token was stored"));
        }

        String plainCodeVerifier = ((PkceParameters) request.getExtraParameters()).getCodeVerifier();
        String expectedChallenge = CryptoHash.hash(plainCodeVerifier);

        if (!Objects.equals(codeChallenge, expectedChallenge)) {
            return Try.failure(new ServiceException(ErrorCode.GENERIC_AUTH_FAILURE,
                    "PKCE code challenge validation failed"));
        }

        return Try.success(true);
    }
}
