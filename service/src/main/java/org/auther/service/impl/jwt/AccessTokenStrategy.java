package org.auther.service.impl.jwt;

import com.auth0.jwt.JWTCreator;
import org.auther.service.JtiProvider;
import org.auther.service.JwtStrategy;
import org.auther.service.impl.jwt.config.ImmutableStrategyConfig;
import org.auther.service.model.AccountBO;
import org.auther.service.model.PermissionBO;

public class AccessTokenStrategy implements JwtStrategy {
    private final ImmutableStrategyConfig strategyConfig;
    private final JtiProvider jti;
    private final TokenGenerator tokenGenerator;

    public AccessTokenStrategy(final ImmutableStrategyConfig strategyConfig, final JtiProvider jti,
                               final TokenGenerator tokenGenerator) {
        this.strategyConfig = strategyConfig;
        this.jti = jti;
        this.tokenGenerator = tokenGenerator;
    }

    @Override
    public JWTCreator.Builder generateToken(final AccountBO account) {
        final JWTCreator.Builder tokenBuilder = tokenGenerator.generateUnsignedToken(account, JwtConfigParser.parseDuration(strategyConfig.getTokenLife()));

        if (strategyConfig.getUseJti()) {
            tokenBuilder.withJWTId(jti.next());
        }

        if (strategyConfig.getIncludePermissions()) {
            tokenBuilder.withArrayClaim("permissions", account.getPermissions().stream()
                    .map(this::permissionToString).toArray(String[]::new));
        }

        if (strategyConfig.getIncludeScopes()) {
            tokenBuilder.withArrayClaim("scopes", account.getScopes().toArray(new String[0]));
        }

        return tokenBuilder;
    }

    @Override
    public String generateRefreshToken(final AccountBO account) {
        return tokenGenerator.generateRandomRefreshToken();
    }

    @Override
    public ImmutableStrategyConfig getConfig() {
        return strategyConfig;
    }

    private String permissionToString(final PermissionBO permission) {
        return permission.getGroup() + "." + permission.getName();
    }
}
