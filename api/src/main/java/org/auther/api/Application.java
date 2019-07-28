package org.auther.api;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.auther.api.routes.UsersRoute;

import static io.javalin.apibuilder.ApiBuilder.path;

public class Application {
    public static void main(final String[] args) {
        final Javalin app = Javalin.create().start(3000);
        final ObjectMapper mapper = new ObjectMapper();

        app.routes(() -> {
            path("/users", new UsersRoute(mapper));
        });

        // if we failed to process a request body
        app.exception(JsonMappingException.class, (e, context) -> {
            e.printStackTrace();
            context.status(422).result("Unprocessable entity");
        });
    }
}