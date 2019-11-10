package org.auther.api;

import com.auther.config.ConfigContext;
import com.auther.config.LightbendConfigContext;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.javalin.Javalin;
import org.auther.api.injectors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private final static Logger log = LoggerFactory.getLogger(Application.class.getSimpleName());

    public static void main(final String[] args) {
        final ConfigContext configContext = new LightbendConfigContext()
                .getSubContext("authguard");

        final Injector injector = Guice.createInjector(new MappersBinder(), new ConfigBinder(),
                new ServicesBinder(), new JwtBinder(configContext), new DalBinder());

        new Server(injector).start(Javalin.create(), 3000);
    }
}
