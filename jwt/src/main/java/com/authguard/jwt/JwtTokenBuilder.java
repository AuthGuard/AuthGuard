package com.authguard.jwt;

import com.auth0.jwt.JWTCreator;

import java.util.Optional;

public class JwtTokenBuilder {
    private final String id;
    private final JWTCreator.Builder builder;

    public JwtTokenBuilder(final String id, final JWTCreator.Builder builder) {
        this.id = id;
        this.builder = builder;
    }

    public Optional<String> getId() {
        return Optional.ofNullable(id);
    }

    public JWTCreator.Builder getBuilder() {
        return builder;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private JWTCreator.Builder builder;

        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        public Builder builder(final JWTCreator.Builder builder) {
            this.builder = builder;
            return this;
        }

        public JwtTokenBuilder build() {
            return new JwtTokenBuilder(id, builder);
        }
    }
}
