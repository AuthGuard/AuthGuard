package com.authguard.service;

import com.authguard.service.model.TokensBO;

import java.util.Optional;

public interface AuthorizationService {
    /**
     * Authorize a user using an ID token.
     * @param header Authorization header (must be Bearer)
     * @return
     */
    TokensBO authorize(String header);

    /**
     * Refresh an access token using the refresh token
     * @param refreshToken
     * @return
     */
    TokensBO refresh(String refreshToken);
}
