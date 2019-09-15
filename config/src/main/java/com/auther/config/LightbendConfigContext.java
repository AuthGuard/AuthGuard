package com.auther.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class LightbendConfigContext implements ConfigContext {
    private final Config config;

    public LightbendConfigContext(final Config config) {
        this.config = config;
    }

    public LightbendConfigContext(){
        this(ConfigFactory.load());
    }

    @Override
    public Object get(final String key) {
        return this.config.getAnyRef(key);
    }

    @Override
    public String getAsString(final String key) {
        return this.config.getString(key);
    }

    @Override
    public boolean getAsBoolean(final String key) {
        return this.config.getBoolean(key);
    }

    @Override
    public ConfigContext getSubContext(final String key) {
        return new LightbendConfigContext(config.getConfig(key));
    }

    @Override
    public String toString() {
        return config.toString();
    }
}
