package org.auther.api.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import org.auther.api.dto.AccountDTO;

import java.io.IOException;
import java.util.UUID;

import static io.javalin.apibuilder.ApiBuilder.post;

public class UsersRoute implements EndpointGroup {
    private final ObjectMapper mapper;

    public UsersRoute(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void addEndpoints() {
        post("/", this::create);
        post("/authenticate", this::authenticate);
    }

    private void create(final Context context) throws IOException {
        final AccountDTO account = mapper.readValue(context.body(), AccountDTO.class);

        // do some work

        context.json(account.withId(UUID.randomUUID().toString()));
    }

    private void authenticate(final Context context) {
        context.status(400).result("You're not very welcome yet");
    }
}
