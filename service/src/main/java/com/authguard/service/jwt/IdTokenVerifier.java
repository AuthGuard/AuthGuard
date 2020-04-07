package com.authguard.service.jwt;

import com.authguard.service.AuthTokenVerfier;

import java.util.Optional;

public class IdTokenVerifier implements AuthTokenVerfier {
    @Override
    public Optional<String> verifyAccountToken(String token) {
        return Optional.empty();
    }
}
