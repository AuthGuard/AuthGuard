package org.auther.service;

public interface JtiProvider {
    String next();
    boolean validate(String jti);
}
