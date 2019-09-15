package org.auther.api.injectors;

import com.google.inject.AbstractModule;
import org.auther.api.routes.RestMapper;
import org.auther.api.routes.RestMapperImpl;
import org.auther.service.impl.mappers.ServiceMapper;
import org.auther.service.impl.mappers.ServiceMapperImpl;

public class MappersBinder extends AbstractModule {

    @Override
    public void configure() {
        bind(RestMapper.class).to(RestMapperImpl.class);
        bind(ServiceMapper.class).to(ServiceMapperImpl.class);
    }
}
