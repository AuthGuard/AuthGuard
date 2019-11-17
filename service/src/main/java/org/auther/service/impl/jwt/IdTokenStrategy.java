package org.auther.service.impl.jwt;

import com.auth0.jwt.JWTCreator;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.auther.service.JtiProvider;
import org.auther.service.JwtStrategy;
import org.auther.service.config.ImmutableStrategyConfig;
import org.auther.service.model.AccountBO;
import org.auther.service.model.TokenBuilderBO;

public class IdTokenStrategy implements JwtStrategy {
    private final ImmutableStrategyConfig strategyConfig;
    private JtiProvider jti;
    private TokenGenerator tokenGenerator;

    @Inject
    public IdTokenStrategy(@Named("idToken") final ImmutableStrategyConfig strategyConfig) {
        this.strategyConfig = strategyConfig;
    }

    // TODO this is a terrible solution but it's just a hack to get around the limitations of DI for now
    public JwtStrategy configure(final JtiProvider jti, final TokenGenerator tokenGenerator) {
        this.jti = jti;
        this.tokenGenerator = tokenGenerator;

        return this;
    }

    @Override
    public TokenBuilderBO generateToken(final AccountBO account) {
        final JWTCreator.Builder jwtBuilder = tokenGenerator
                .generateUnsignedToken(account, JwtConfigParser.parseDuration(strategyConfig.getTokenLife()));

        final String id = jti.next();
        jwtBuilder.withJWTId(id);

        return TokenBuilderBO.builder()
                .id(id)
                .builder(jwtBuilder)
                .build();
    }

    @Override
    public String generateRefreshToken(final AccountBO account) {
        return null;
    }

    @Override
    public ImmutableStrategyConfig getConfig() {
        return strategyConfig;
    }
}
