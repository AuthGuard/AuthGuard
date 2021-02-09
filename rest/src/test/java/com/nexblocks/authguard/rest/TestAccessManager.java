package com.nexblocks.authguard.rest;

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
        handler.handle(context);
    }
}
