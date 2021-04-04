package com.nexblocks.authguard.service.keys;

public interface ApiKeyHash {
    String hash(String key);
    boolean verify(String plain, String hashed);
}
