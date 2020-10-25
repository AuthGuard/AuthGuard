package com.authguard.api.routes;

import io.javalin.apibuilder.EndpointGroup;

public interface ApiRoute extends EndpointGroup {
    /**
     * The root path of the route without a slash in
     * the beginning.
     */
    String getPath();
}
