package org.auther.api.injectors;

import com.google.inject.AbstractModule;
import org.auther.service.JtiProvider;
import org.auther.service.JwtProvider;
import org.auther.service.impl.jwt.BasicJtiProvider;
import org.auther.service.impl.jwt.JwtProviderImpl;

public class JwtBinder extends AbstractModule {

    @Override
    public void configure() {
        bind(JtiProvider.class).to(BasicJtiProvider.class);
        bind(JwtProvider.class).to(JwtProviderImpl.class);
    }

}
