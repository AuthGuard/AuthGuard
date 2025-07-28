package com.nexblocks.authguard.jwt.oauth.service;

import okhttp3.HttpUrl;

public class ClientUrlUtils {
    public static String getStandardBaseUrl(String url) {
        HttpUrl parsedUrl = HttpUrl.parse(url);
        boolean includePort = parsedUrl.isHttps() ?
                parsedUrl.port() != 443 :
                parsedUrl.port() != 80;

        if (includePort) {
            return parsedUrl.scheme() + "://" + parsedUrl.host() + ":" + parsedUrl.port();
        }

        return parsedUrl.scheme() + "://" + parsedUrl.host();
    }

    public static String getStandardBaseUrl(HttpUrl parsedUrl) {
        boolean includePort = parsedUrl.isHttps() ?
                parsedUrl.port() != 443 :
                parsedUrl.port() != 80;

        if (includePort) {
            return parsedUrl.scheme() + "://" + parsedUrl.host() + ":" + parsedUrl.port();
        }

        return parsedUrl.scheme() + "://" + parsedUrl.host();
    }
}
