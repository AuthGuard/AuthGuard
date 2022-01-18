package com.nexblocks.authguard.rest.routes;

import com.nexblocks.authguard.api.routes.GeneralApi;
import io.javalin.http.Context;

public class GeneralRoute extends GeneralApi {
    @Override
    public void heartbeat(final Context context) {
        context.status(200).result("");
    }
}
