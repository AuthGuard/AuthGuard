package com.nexblocks.authguard.rest;

import com.nexblocks.authguard.api.access.AuthGuardRoles;
import com.nexblocks.authguard.service.model.AppBO;
import com.nexblocks.authguard.service.model.Client;
import com.nexblocks.authguard.service.model.ClientBO;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.Role;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class TestAccessManager implements AccessManager {
    @Override
    public void manage(@NotNull final Handler handler, @NotNull final Context context,
                       @NotNull final Set<Role> set) throws Exception {
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

        handler.handle(context);
    }
}
