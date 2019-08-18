package org.auther.api;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.javalin.Javalin;
import org.auther.api.routes.PermissionsRoute;
import org.auther.api.routes.UsersRoute;

import static io.javalin.apibuilder.ApiBuilder.path;

public class Application {
    public static void main(final String[] args) {
        final Javalin app = Javalin.create().start(3000);
        final Injector injector = Guice.createInjector(new InjectorModule());

        app.routes(() -> {
            path("/users", injector.getInstance(UsersRoute.class));
            path("/admin", injector.getInstance(PermissionsRoute.class));
        });

        // if we failed to process a request body
        app.exception(JsonMappingException.class, (e, context) -> context.status(422).result("Unprocessable entity"));
    }
}
