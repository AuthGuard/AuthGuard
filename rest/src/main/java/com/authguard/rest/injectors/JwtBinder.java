package com.authguard.rest.injectors;

import com.authguard.config.ConfigContext;
import com.authguard.service.config.ImmutableJwtConfig;
import com.authguard.service.jwt.BasicJtiProvider;
import com.google.inject.AbstractModule;
import com.authguard.service.jwt.JtiProvider;

public class JwtBinder extends AbstractModule {
    private final ImmutableJwtConfig jwtConfig;

    public JwtBinder(final ConfigContext configContext) {
        this.jwtConfig = configContext.getAsConfigBean("jwt", ImmutableJwtConfig.class);
    }

    @Override
    public void configure() {
        bind(JtiProvider.class).to(BasicJtiProvider.class);
        bind(ImmutableJwtConfig.class).toInstance(jwtConfig);
    }

}
