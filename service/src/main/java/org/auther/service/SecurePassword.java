package org.auther.service;

public interface SecurePassword {
    String hash(String plain);
    boolean verify(String plain, String hashed);
}
