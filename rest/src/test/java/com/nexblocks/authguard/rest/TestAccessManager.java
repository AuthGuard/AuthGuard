package com.nexblocks.authguard.rest;

import com.nexblocks.authguard.service.model.Client;
import com.nexblocks.authguard.service.model.ClientBO;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

public class TestAccessManager implements Handler {
    @Override
    public void handle(@NotNull final Context context) throws Exception {
        final ClientBO client;

        if ("auth-client".equals(context.header("Authorization"))) {
            client = ClientBO.builder()
                    .id(201)
                    .clientType(Client.ClientType.AUTH)
                    .domain("test")
                    .build();
        } else {
            client = ClientBO.builder()
                    .id(201)
                    .clientType(Client.ClientType.ADMIN)
                    .build();
        }

        context.attribute("actor", client);
    }
}
