package org.auther.service;

import com.auth0.jwt.JWTCreator;
import org.auther.service.impl.jwt.config.ImmutableStrategyConfig;
import org.auther.service.model.AccountBO;

public interface JwtStrategy {
    JWTCreator.Builder generateToken(AccountBO account);
    String generateRefreshToken(AccountBO accountBO);

    ImmutableStrategyConfig getConfig();
}
