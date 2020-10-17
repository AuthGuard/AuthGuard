package com.authguard.rest.server;

import com.authguard.config.ConfigContext;
import com.authguard.rest.routes.*;
import com.google.inject.Injector;
import io.javalin.Javalin;

import static io.javalin.apibuilder.ApiBuilder.path;

public class ServerRoutesHandlers implements JavalinAppConfigurer {
    private final Injector injector;
    private final ConfigContext config;

    public ServerRoutesHandlers(final Injector injector, final ConfigContext config) {
        this.injector = injector;
        this.config = config;
    }

    @Override
    public void configure(final Javalin app) {
        app.routes(() -> {
            path("/credentials", injector.getInstance(CredentialsRoute.class));
            path("/auth", injector.getInstance(AuthRoute.class));
            path("/keys", injector.getInstance(ApiKeysRoute.class));
            path("/accounts", injector.getInstance(AccountsRoute.class));
            path("/apps", injector.getInstance(ApplicationsRoute.class));
            path("/admin", injector.getInstance(AdminRoute.class));
            path("/roles", injector.getInstance(RolesRoute.class));
            path("/permissions", injector.getInstance(PermissionsRoute.class));

            if (config.get("otp") != null) {
                path("/otp", injector.getInstance(OtpRoute.class));
            }

            if (config.get("verification") != null) {
                path("/verification", injector.getInstance(VerificationRoute.class));
            }

            if (config.get("passwordless") != null) {
                path("/passwordless", injector.getInstance(PasswordlessRoute.class));
            }
        });
    }
}
