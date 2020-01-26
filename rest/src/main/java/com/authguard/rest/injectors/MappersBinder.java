package com.authguard.rest.injectors;

import com.authguard.rest.routes.RestMapper;
import com.google.inject.AbstractModule;
import com.authguard.rest.routes.RestMapperImpl;
import com.authguard.service.impl.mappers.ServiceMapper;
import com.authguard.service.impl.mappers.ServiceMapperImpl;

public class MappersBinder extends AbstractModule {

    @Override
    public void configure() {
        bind(RestMapper.class).to(RestMapperImpl.class);
        bind(ServiceMapper.class).to(ServiceMapperImpl.class);
    }
}
