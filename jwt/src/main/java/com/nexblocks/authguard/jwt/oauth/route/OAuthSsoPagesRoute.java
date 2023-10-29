package com.nexblocks.authguard.jwt.oauth.route;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.nexblocks.authguard.api.annotations.DependsOnConfiguration;
import com.nexblocks.authguard.api.dto.entities.RequestValidationError;
import com.nexblocks.authguard.api.routes.ApiRoute;
import com.nexblocks.authguard.config.ConfigContext;
import com.nexblocks.authguard.jwt.oauth.config.ImmutableOAuthSsoConfiguration;
import com.nexblocks.authguard.jwt.oauth.config.OAuthSsoConfiguration;
import com.nexblocks.authguard.service.exceptions.ConfigurationException;
import io.javalin.http.Context;
import io.vavr.control.Either;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.javalin.apibuilder.ApiBuilder.get;

@DependsOnConfiguration("oauthSso")
public class OAuthSsoPagesRoute implements ApiRoute {
    private final OAuthSsoConfiguration configuration;

    private final String loginPage;
    private final String otpPage;

    @Inject
    public OAuthSsoPagesRoute(final @Named("oauthSso") ConfigContext configContext) {
        configuration = configContext.asConfigBean(ImmutableOAuthSsoConfiguration.class);

        if (!(configuration.useEmail() || configuration.useUsername() || configuration.usePhoneNumber())) {
            throw new ConfigurationException("SSO must be allowed with at least one identifier type. " +
                    "Set one or more of useUsername, useEmail, or setPhoneNumber to true.");
        }

        loginPage = replaceParameters(readHtmlPage(configuration.getLoginPage()), configuration);
        otpPage = readHtmlPage(configuration.getOtpPage());
    }

    @Override
    public String getPath() {
        return "sso";
    }

    @Override
    public void addEndpoints() {
        get("/auth", this::loginPage);
        get("/otp", this::otpPage);
    }

    private void loginPage(Context context) {
        final Either<RequestValidationError, ImmutableOpenIdConnectRequest> request
                = OpenIdConnectRequestParser.fromContext(context, "code");

        if (request.isLeft()) {
            context.status(400).json(request.getLeft());
        } else {
            context.status(200).html(loginPage);
        }
    }

    private void otpPage(Context context) {
        context.status(200).html(otpPage);
    }

    private String readHtmlPage(final String path) {
        InputStream stream = resolveFilePath(path);

        try {
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                BufferedReader bufferedReader = new BufferedReader(reader);

                return bufferedReader.lines().collect(Collectors.joining(" "));
            }
        } catch (Exception e) {
            throw new ConfigurationException("Failed to read file " + path, e);
        }
    }

    private InputStream resolveFilePath(final String path) {
        if (path.startsWith("resources/")) {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            String resourceName = path.substring(path.indexOf("/") + 1);

            InputStream stream = classloader.getResourceAsStream(resourceName);

            if (stream == null) {
                throw new ConfigurationException("Resource file " + path + "resolved to nothing");
            }

            return stream;
        }

        try {
            return new FileInputStream(path);
        } catch (FileNotFoundException e) {
            throw new ConfigurationException("Failed to resolve file " + path, e);
        }
    }

    private String replaceParameters(final String page, final OAuthSsoConfiguration config) {
        String identifierPlaceholder = Stream.of(config.useUsername() ? "username" : null,
                        config.useEmail() ? "email" : null,
                        config.usePhoneNumber() ? "phone number" : null)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));

        return page.replace("${identifierPlaceholder}", StringUtils.capitalize(identifierPlaceholder));
    }
}
