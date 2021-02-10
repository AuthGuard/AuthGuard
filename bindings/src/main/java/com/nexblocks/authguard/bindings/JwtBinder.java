package com.nexblocks.authguard.bindings;

import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.service.config.JwtConfig;
import com.nexblocks.authguard.jwt.BasicJtiProvider;
import com.google.inject.AbstractModule;
import com.nexblocks.authguard.jwt.JtiProvider;

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
