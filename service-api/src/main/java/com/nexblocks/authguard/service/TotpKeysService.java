package com.nexblocks.authguard.service;

import com.nexblocks.authguard.service.model.TotpKeyBO;

import java.util.List;
import java.util.Optional;
import io.smallrye.mutiny.Uni;

public interface TotpKeysService extends CrudService<TotpKeyBO> {
    default Uni<TotpKeyBO> create(TotpKeyBO entity) {
        throw new UnsupportedOperationException("Time-based OTP keys cannot be created using 'create()'. Use 'generate()' instead.");
    }

    default Uni<Optional<TotpKeyBO>> update(TotpKeyBO entity, String domain) {
        throw new UnsupportedOperationException("Time-based OTP keys cannot be updated");
    }

    /**
     * Generates and stores a TOTP key for an account, and optionally for
     * a particular authenticator. If no authenticator is provided then
     * the default values will be used while verifying the generated
     * OTP.
     */
    Uni<TotpKeyBO> generate(long accountId, String domain, String authenticator);
    Uni<List<TotpKeyBO>> getByAccountId(long accountId, String domain);
    Uni<Optional<TotpKeyBO>> getByAccountIdDecrypted(long accountId, String domain);
}
