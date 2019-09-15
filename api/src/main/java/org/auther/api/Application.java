package org.auther.api;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.javalin.Javalin;
import org.auther.api.injectors.*;
import org.auther.api.routes.AdminRoute;
import org.auther.api.routes.AuthenticationRoute;
import org.auther.api.routes.UsersRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.javalin.apibuilder.ApiBuilder.path;

public class Application {
    private final static Logger log = LoggerFactory.getLogger(Application.class.getSimpleName());

    public static void main(final String[] args) {
        final Javalin app = Javalin.create()
                .start(3000);

        final Injector injector = Guice.createInjector(new MappersBinder(), new ConfigBinder(),
                new ServicesBinder(), new JWTBinder(), new DALBinder());

        app.before(context -> context.attribute("time", System.currentTimeMillis()));

        app.after(context -> {
            final Long now = System.currentTimeMillis();
            final Long start = context.attribute("time");

            if (start == null) {
                log.info("{} {} - {}", context.method(), context.path(), context.status());
            } else {
                log.info("{} {} - {} {} ms", context.method(), context.path(), context.status(), now - start);
            }
        });

        app.routes(() -> {
            path("/authenticate", injector.getInstance(AuthenticationRoute.class));
            path("/users", injector.getInstance(UsersRoute.class));
            path("/admin", injector.getInstance(AdminRoute.class));
        });

        // if we failed to process a request body
        app.exception(JsonMappingException.class, (e, context) -> context.status(422).result("Unprocessable entity"));
    }
}
