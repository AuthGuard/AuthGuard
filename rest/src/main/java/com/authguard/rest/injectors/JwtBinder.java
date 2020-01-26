package com.authguard.rest.injectors;

import com.authguard.config.ConfigContext;
import com.authguard.service.config.ImmutableJwtConfig;
import com.authguard.service.config.ImmutableStrategiesConfig;
import com.authguard.service.config.ImmutableStrategyConfig;
import com.authguard.service.impl.jwt.AccessTokenProvider;
import com.authguard.service.impl.jwt.BasicJtiProvider;
import com.authguard.service.impl.jwt.IdTokenProvider;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.authguard.service.JtiProvider;
import com.authguard.service.JwtProvider;

public class JwtBinder extends AbstractModule {
    private final ImmutableJwtConfig jwtConfig;
    private final ImmutableStrategiesConfig strategiesConfig;
    private final String authenticationStrategy;
    private final String authorizationStrategy;

    public JwtBinder(final ConfigContext configContext) {
        this.jwtConfig = configContext.getAsConfigBean("jwt", ImmutableJwtConfig.class);
        this.strategiesConfig = configContext.getSubContext("jwt")
                .getAsConfigBean("strategies", ImmutableStrategiesConfig.class);

        this.authenticationStrategy = configContext.getSubContext("authentication").getAsString("strategy");
        this.authorizationStrategy = configContext.getSubContext("authorization").getAsString("strategy");
    }

    @Override
    public void configure() {
        bind(JtiProvider.class).to(BasicJtiProvider.class);
        bind(ImmutableJwtConfig.class).toInstance(jwtConfig);

        bind(ImmutableStrategyConfig.class)
                .annotatedWith(Names.named("idToken"))
                .toInstance(strategiesConfig.getIdToken());

        bind(ImmutableStrategyConfig.class)
                .annotatedWith(Names.named("accessToken"))
                .toInstance(strategiesConfig.getAccessToken());

        switch (authenticationStrategy) {
            case "idToken":
                bind(JwtProvider.class)
                        .annotatedWith(Names.named("authenticationTokenProvider"))
                        .to(IdTokenProvider.class);
                break;

            case "accessToken":
                bind(JwtProvider.class)
                        .annotatedWith(Names.named("authenticationTokenProvider"))
                        .to(AccessTokenProvider.class);
                break;
        }

        switch (authorizationStrategy) {
            case "accessToken":
                bind(JwtProvider.class)
                        .annotatedWith(Names.named("authorizationTokenProvider"))
                        .to(AccessTokenProvider.class);
                break;
        }
    }

}
