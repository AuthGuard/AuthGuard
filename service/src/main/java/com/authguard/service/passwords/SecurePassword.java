package com.authguard.service.passwords;

import com.authguard.service.model.HashedPasswordBO;

/**
 * An interface for salting and hashing passwords.
 *
 * @see SCryptPassword
 * @see BCryptPassword
 */
public interface SecurePassword {
    HashedPasswordBO hash(String plain);
    boolean verify(String plain, HashedPasswordBO hashed);
}
