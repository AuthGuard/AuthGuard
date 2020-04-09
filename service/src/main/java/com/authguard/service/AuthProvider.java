package com.authguard.service;

import com.authguard.service.model.AccountBO;
import com.authguard.service.model.AppBO;
import com.authguard.service.model.TokensBO;

public interface AuthProvider {
    /**
     * Generate a token and (optionally) a refresh token.
     * @param account The account for which the tokens
     *                will be generated.
     * @return The generated tokens.
     */
    TokensBO generateToken(AccountBO account);

    /**
     * Generate a token for an app.
     */
    TokensBO generateToken(AppBO app);
}
