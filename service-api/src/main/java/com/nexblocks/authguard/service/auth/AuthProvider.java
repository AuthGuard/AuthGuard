package com.nexblocks.authguard.service.auth;

import com.nexblocks.authguard.service.model.*;

public interface AuthProvider {
    /**
     * Generate a token and (optionally) a refresh token.
     */
    TokensBO generateToken(AccountBO account);

    default TokensBO generateToken(AccountBO account, TokenRestrictionsBO restrictions) {
        return generateToken(account);
    }

    TokensBO generateToken(AppBO app);

    default TokensBO delete(AuthRequestBO authRequest) {
        throw new UnsupportedOperationException("Token cannot be deleted");
    }
}
