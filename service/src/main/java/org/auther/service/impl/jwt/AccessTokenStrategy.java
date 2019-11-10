package org.auther.service.impl.jwt;

import com.auth0.jwt.JWTCreator;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.auther.service.JtiProvider;
import org.auther.service.JwtStrategy;
import org.auther.service.config.ImmutableStrategyConfig;
import org.auther.service.model.AccountBO;
import org.auther.service.model.PermissionBO;
import org.auther.service.model.TokenBuilderBO;

public class AccessTokenStrategy implements JwtStrategy {
    private final ImmutableStrategyConfig strategyConfig;
    private JtiProvider jti;
    private TokenGenerator tokenGenerator;

    @Inject
    public AccessTokenStrategy(@Named("accessToken") final ImmutableStrategyConfig strategyConfig) {
        this.strategyConfig = strategyConfig;
    }

    @Override
    public JwtStrategy configure(final JtiProvider jti, final TokenGenerator tokenGenerator) {
        this.jti = jti;
        this.tokenGenerator = tokenGenerator;

        return this;
    }

    @Override
    public TokenBuilderBO generateToken(final AccountBO account) {
        final TokenBuilderBO.Builder tokenBuilder = TokenBuilderBO.builder();
        final JWTCreator.Builder jwtBuilder = tokenGenerator.generateUnsignedToken(account,
                JwtConfigParser.parseDuration(strategyConfig.getTokenLife()));

        if (strategyConfig.getUseJti()) {
            final String id = jti.next();
            jwtBuilder.withJWTId(id);
            tokenBuilder.id(id);
        }

        if (strategyConfig.getIncludePermissions()) {
            jwtBuilder.withArrayClaim("permissions", account.getPermissions().stream()
                    .map(this::permissionToString).toArray(String[]::new));
        }

        if (strategyConfig.getIncludeScopes()) {
            jwtBuilder.withArrayClaim("scopes", account.getScopes().toArray(new String[0]));
        }

        return tokenBuilder.builder(jwtBuilder).build();
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
