package com.nexblocks.authguard.api.routes;

public class RoutesConstants {
    public static final String RES_CSRF_COOKIE = "CSRF-TOKEN";
    public static final String REQ_CSRF_HEADER = "X-CSRF-TOKEN";
    public static final String AUTH_SESSION_TOKEN_COOKIE = "AST";

    public static final String CspHeaderValue = String.join(" ",
            "default-src 'self';",
            "script-src 'self';",
            "style-src 'self' 'unsafe-inline';",
            "connect-src 'self';",
            "img-src 'self' data:;",
            "form-action 'self';",
            "base-uri 'self';",
            "frame-ancestors 'none';"
    );
}
