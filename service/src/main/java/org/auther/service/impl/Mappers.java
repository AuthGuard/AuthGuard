package org.auther.service.impl;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.auther.dal.model.AccountDO;
import org.auther.service.model.AccountBO;

public class Mappers {
    private static final MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();

    static {
        mapperFactory.classMap(AccountDO.class, AccountBO.class).byDefault().register();
        mapperFactory.classMap(AccountBO.class, AccountDO.class).byDefault().register();
    }

    static MapperFacade getMapper() {
        return mapperFactory.getMapperFacade();
    }
}
