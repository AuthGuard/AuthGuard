package org.auther.rest.routes;

import com.auther.config.ConfigContext;
import com.google.inject.Inject;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.get;

public class AdminRoute implements EndpointGroup {
    private final ConfigContext configContext;

    @Inject
    public AdminRoute(final ConfigContext configContext) {
        this.configContext = configContext;
    }

    @Override
    public void addEndpoints() {
        get("/config", this::getConfig);
    }

    private void getConfig(final Context context) {
        context.status(200).json(this.configContext.get("authguard"));
    }
}
