package org.auther.service.impl.jwt;

import com.google.inject.Inject;
import org.auther.service.JtiProvider;
import org.auther.service.config.ImmutableJwtConfig;

public class IdTokenProvider extends JwtProviderImpl {

    @Inject
    public IdTokenProvider(final ImmutableJwtConfig jwtConfig, final IdTokenStrategy strategy,
                           final JtiProvider jti) {
        super(jwtConfig, strategy, jti);
    }
}
