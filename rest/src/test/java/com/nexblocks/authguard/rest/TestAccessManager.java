package com.nexblocks.authguard.rest;

import com.nexblocks.authguard.api.access.AuthGuardRoles;
import com.nexblocks.authguard.service.model.AppBO;
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
        final AppBO app;

        if ("auth-client".equals(context.header("Authorization"))) {
            app = AppBO.builder()
                    .id("valid-test-app")
                    .addRoles(AuthGuardRoles.AUTH_CLIENT)
                    .domain("test")
                    .build();
        } else {
            app = AppBO.builder()
                    .id("valid-test-app")
                    .addRoles(AuthGuardRoles.ADMIN_CLIENT)
                    .build();
        }

        context.attribute("actor", app);

        handler.handle(context);
    }
}
