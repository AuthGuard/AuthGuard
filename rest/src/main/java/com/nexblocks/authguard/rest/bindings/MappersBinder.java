package com.nexblocks.authguard.rest.bindings;

import com.nexblocks.authguard.rest.mappers.RestMapper;
import com.nexblocks.authguard.rest.mappers.RestMapperImpl;
import com.nexblocks.authguard.service.mappers.ServiceMapper;
import com.nexblocks.authguard.service.mappers.ServiceMapperImpl;
import com.google.inject.AbstractModule;

public class MappersBinder extends AbstractModule {

    @Override
    public void configure() {
        bind(RestMapper.class).to(RestMapperImpl.class);
        bind(ServiceMapper.class).to(ServiceMapperImpl.class);
    }
}
