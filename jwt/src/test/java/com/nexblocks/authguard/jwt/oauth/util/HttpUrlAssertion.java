package com.nexblocks.authguard.jwt.oauth.util;

import okhttp3.HttpUrl;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpUrlAssertion {
    public static void assertAuthorizationUrl(final HttpUrl actual, final HttpUrl expected, final String... excludeParameters) {
        assertThat(actual.scheme()).isEqualTo(expected.scheme());
        assertThat(actual.host()).isEqualTo(expected.host());
        assertThat(actual.port()).isEqualTo(expected.port());
        assertThat(actual.pathSegments()).isEqualTo(expected.pathSegments());

        final Set<String> exclusions = Stream.of(excludeParameters).collect(Collectors.toSet());

        Stream.concat(actual.queryParameterNames().stream(), expected.queryParameterNames().stream())
                .filter(param -> !exclusions.contains(param))
                .forEach(param -> assertThat(actual.queryParameter(param)).isEqualTo(expected.queryParameter(param)));

    }
}
