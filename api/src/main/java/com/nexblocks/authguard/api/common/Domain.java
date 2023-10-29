package com.nexblocks.authguard.api.common;

import io.javalin.http.Context;

public class Domain {
    public static String fromContext(final Context context) {
        return context.pathParam("domain");
    }
}
