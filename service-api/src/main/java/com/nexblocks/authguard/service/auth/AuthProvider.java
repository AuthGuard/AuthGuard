package com.nexblocks.authguard.service.auth;

import com.nexblocks.authguard.service.model.AccountBO;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.TokenRestrictionsBO;
import com.nexblocks.authguard.service.model.TokensBO;

public interface AuthProvider {
    /**
     * Generate a token and (optionally) a refresh token.
     */
    TokensBO generateToken(AccountBO account);

    default TokensBO generateToken(AccountBO account, TokenRestrictionsBO restrictions) {
        return generateToken(account);
    }

    TokensBO generateToken(AppBO app);
}
