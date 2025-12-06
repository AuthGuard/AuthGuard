package com.nexblocks.authguard.saml;

import okhttp3.HttpUrl;

import java.util.Objects;


public final class AcsUrlValidator {

    private AcsUrlValidator() {
    }

    public static boolean isValidAcsUrl(final HttpUrl requestedAcsUrl,
                                        final String registeredAcsUrl) {
        if (requestedAcsUrl == null
                || registeredAcsUrl == null
                || registeredAcsUrl.isEmpty()) {
            return false;
        }

        HttpUrl allowed = HttpUrl.parse(registeredAcsUrl);
        if (allowed == null) {
            return false;
        }

        return urlsMatch(requestedAcsUrl, allowed);
    }

    private static boolean urlsMatch(final HttpUrl req, final HttpUrl allowed) {
        if (!Objects.equals(req.scheme(), allowed.scheme())) {
            return false;
        }

        if (!Objects.equals(req.host(), allowed.host())) {
            return false;
        }

        int reqPort = req.port() == -1 ? defaultPort(req.scheme()) : req.port();
        int allowedPort = allowed.port() == -1 ? defaultPort(allowed.scheme()) : allowed.port();
        if (reqPort != allowedPort) {
            return false;
        }

        return Objects.equals(req.encodedPath(), allowed.encodedPath());
    }

    private static int defaultPort(String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 :
                "http".equalsIgnoreCase(scheme) ? 80 : -1;
    }
}
