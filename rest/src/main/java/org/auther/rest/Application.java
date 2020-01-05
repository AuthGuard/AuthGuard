package org.auther.rest;

import com.auther.config.ConfigContext;
import com.auther.config.JacksonConfigContext;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.javalin.Javalin;
import org.auther.rest.access.RolesAccessManager;
import org.auther.rest.injectors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Application {
    private final static Logger log = LoggerFactory.getLogger(Application.class.getSimpleName());

    public static void main(final String[] args) {
        final ConfigContext configContext = new JacksonConfigContext(
                new File(Application.class.getClassLoader().getResource("application.json").getFile())
        ).getSubContext(ConfigContext.ROOT_CONFIG_PROPERTY);

        final Injector injector = Guice.createInjector(new MappersBinder(), new ConfigBinder(configContext),
                new ServicesBinder(), new JwtBinder(configContext), new DalBinder(), new EmbBinder());

        // run bootstraps
        injector.getInstance(Bootstrap.class).bootstrapOneTimeAdmin();

        // run the server
        new Server(injector).start(Javalin.create(config -> {
            config.accessManager(new RolesAccessManager());
        }), 3000);
    }
}
