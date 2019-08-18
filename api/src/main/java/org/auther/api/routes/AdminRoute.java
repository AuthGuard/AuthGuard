package org.auther.api.routes;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;

import static io.javalin.apibuilder.ApiBuilder.get;

public class AdminRoute implements EndpointGroup {
    @Override
    public void addEndpoints() {
        get("/config", this::getConfig);
    }

    private void getConfig(final Context context) {
        context.status(401).result("You're not welcome here yet");
    }
}
