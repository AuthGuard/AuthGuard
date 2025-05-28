package com.nexblocks.authguard.service.auth;

import com.nexblocks.authguard.service.model.*;

import java.time.Instant;
import io.smallrye.mutiny.Uni;

public interface AuthProvider {
    /**
     * Generate a token and (optionally) a refresh token.
     */
    default Uni<AuthResponseBO> generateToken(AccountBO account) {
        return generateToken(account, null, null);
    }

    default Uni<AuthResponseBO> generateToken(AccountBO account, TokenOptionsBO options) {
        return generateToken(account, null, options);
    }

    default Uni<AuthResponseBO> generateToken(AccountBO account, TokenRestrictionsBO restrictions) {
        return generateToken(account, restrictions, null);
    }

    Uni<AuthResponseBO> generateToken(AccountBO account, TokenRestrictionsBO restrictions, TokenOptionsBO options);

    AuthResponseBO generateToken(AppBO app);

    default AuthResponseBO generateToken(ClientBO client) {
        throw new UnsupportedOperationException("Cannot generate token for a client");
    }

    default AuthResponseBO generateToken(AppBO app, Instant expiresAt) {
        throw new UnsupportedOperationException("Cannot generate token with expiry");
    }

    default AuthResponseBO generateToken(ClientBO client, Instant expiresAt) {
        throw new UnsupportedOperationException("Cannot generate token with expiry");
    }

    default Uni<AuthResponseBO> delete(AuthRequestBO authRequest) {
        throw new UnsupportedOperationException("Token cannot be deleted");
    }
}
