package com.nexblocks.authguard.jwt.oauth.route;

class RoutesConstants {
    static final String RES_CSRF_COOKIE = "CSRF-TOKEN";
    static final String REQ_CSRF_HEADER = "X-CSRF-TOKEN";

    static final String CspHeaderValue = String.join(" ",
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
