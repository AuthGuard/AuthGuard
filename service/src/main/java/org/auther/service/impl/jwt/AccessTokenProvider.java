package org.auther.service.impl.jwt;

import com.google.inject.Inject;
import org.auther.service.JtiProvider;
import org.auther.service.config.ImmutableJwtConfig;

public class AccessTokenProvider extends JwtProviderImpl {

    @Inject
    public AccessTokenProvider(final ImmutableJwtConfig jwtConfig, final AccessTokenStrategy strategy,
                               final JtiProvider jti) {
        super(jwtConfig, strategy, jti);
    }
}
