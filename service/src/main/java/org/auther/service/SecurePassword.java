package org.auther.service;

import org.auther.service.model.HashedPasswordBO;

/**
 * An interface for salting and hashing passwords.
 *
 * @see org.auther.service.impl.passwords.SCryptPassword
 * @see org.auther.service.impl.passwords.BCryptPassword
 */
public interface SecurePassword {
    HashedPasswordBO hash(String plain);
    boolean verify(String plain, HashedPasswordBO hashed);
}
