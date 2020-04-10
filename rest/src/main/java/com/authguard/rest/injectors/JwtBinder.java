package com.authguard.rest.injectors;

import com.authguard.config.ConfigContext;
import com.authguard.service.config.JwtConfig;
import com.authguard.service.jwt.BasicJtiProvider;
import com.google.inject.AbstractModule;
import com.authguard.service.jwt.JtiProvider;

public class JwtBinder extends AbstractModule {
    private final JwtConfig jwtConfig;

    public JwtBinder(final ConfigContext configContext) {
        this.jwtConfig = configContext.getAsConfigBean("jwt", JwtConfig.class);
    }

    @Override
    public void configure() {
        bind(JtiProvider.class).to(BasicJtiProvider.class);
        bind(JwtConfig.class).toInstance(jwtConfig);
    }

}
