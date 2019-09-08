package com.auther.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Created by tao on 8/15/2019.
 */
public class LightbendConfigContext implements ConfigContext {

    private Config config;

    public LightbendConfigContext(Config config){
        this.config = config;
    }

    public LightbendConfigContext(){
        this(ConfigFactory.load());
    }

    @Override
    public Object get(String key) {
        return this.config.getAnyRef(key);
    }

    @Override
    public String toString() {
        return config.getConfig("jwt").toString();
    }
}
