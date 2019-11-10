package org.auther.service;

import org.auther.service.impl.jwt.TokenGenerator;
import org.auther.service.config.ImmutableStrategyConfig;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokenBuilderBO;

public interface JwtStrategy {
    TokenBuilderBO generateToken(AccountBO account);
    String generateRefreshToken(AccountBO account);
    JwtStrategy configure(JtiProvider jti, TokenGenerator tokenGenerator);

    // TODO this is a design flaw and needs to be fixed, the strategy should not have to expose its config
    ImmutableStrategyConfig getConfig();
}
