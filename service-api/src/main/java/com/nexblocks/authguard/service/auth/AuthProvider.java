package com.nexblocks.authguard.service.auth;

import com.nexblocks.authguard.service.model.*;

public interface AuthProvider {
    /**
     * Generate a token and (optionally) a refresh token.
     */
    AuthResponseBO generateToken(AccountBO account);

    default AuthResponseBO generateToken(AccountBO account, TokenOptionsBO options) {
        return generateToken(account);
    }

    default AuthResponseBO generateToken(AccountBO account, TokenRestrictionsBO restrictions) {
        return generateToken(account);
    }

    default AuthResponseBO generateToken(AccountBO account, TokenRestrictionsBO restrictions, TokenOptionsBO options) {
        return generateToken(account);
    }

    AuthResponseBO generateToken(AppBO app);

    default AuthResponseBO delete(AuthRequestBO authRequest) {
        throw new UnsupportedOperationException("Token cannot be deleted");
    }
}
