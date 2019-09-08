package org.auther.service;

import org.auther.service.model.HashedPasswordBO;

public interface SecurePassword {
    HashedPasswordBO hash(String plain);
    boolean verify(String plain, HashedPasswordBO hashed);
}
