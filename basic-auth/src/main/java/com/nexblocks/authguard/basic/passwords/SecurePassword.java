package com.nexblocks.authguard.basic.passwords;

import com.nexblocks.authguard.service.model.HashedPasswordBO;
import io.smallrye.mutiny.Uni;

/**
 * An interface for salting and hashing passwords.
 *
 * @see SCryptPassword
 * @see BCryptPassword
 */
public interface SecurePassword {
    Uni<HashedPasswordBO> hash(String plain);
    Uni<Boolean> verify(String plain, HashedPasswordBO hashed);
}
