package org.auther.service;

import org.auther.service.model.TokensBO;

import java.util.Optional;

public interface AuthenticationService {
    Optional<TokensBO> authenticate(String authHeader);
}
