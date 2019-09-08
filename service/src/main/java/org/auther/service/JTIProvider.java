package org.auther.service;

public interface JTIProvider {
    String next();
    boolean validate(String jti);
}
