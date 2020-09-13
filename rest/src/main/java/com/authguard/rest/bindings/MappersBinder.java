package com.authguard.rest.bindings;

import com.authguard.rest.mappers.RestMapper;
import com.authguard.rest.mappers.RestMapperImpl;
import com.google.inject.AbstractModule;
import com.authguard.service.mappers.ServiceMapper;
import com.authguard.service.mappers.ServiceMapperImpl;

public class MappersBinder extends AbstractModule {

    @Override
    public void configure() {
        bind(RestMapper.class).to(RestMapperImpl.class);
        bind(ServiceMapper.class).to(ServiceMapperImpl.class);
    }
}
