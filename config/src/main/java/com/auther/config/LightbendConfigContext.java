package com.auther.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
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
    public <T> T getAsConfigBean(final String key, final Class<T> clazz) {
        return ConfigBeanFactory.create(config.getConfig(key), clazz);
    }

    @Override
    public <T> T asConfigBean(final Class<T> clazz) {
        return ConfigBeanFactory.create(config, clazz);
    }

    @Override
    public String toString() {
        return config.toString();
    }
}
