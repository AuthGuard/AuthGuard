package com.nexblocks.authguard.rest.util;

import com.nexblocks.authguard.api.dto.entities.DomainScoped;
import com.nexblocks.authguard.api.dto.validation.Validator;
import com.nexblocks.authguard.api.dto.validation.validators.Validators;
import com.nexblocks.authguard.api.dto.validation.violations.Violation;
import com.nexblocks.authguard.rest.access.EntityDomainChecker;
import com.nexblocks.authguard.rest.exceptions.RequestValidationException;
import com.nexblocks.authguard.rest.mappers.RestJsonMapper;
import io.javalin.http.Context;

import java.util.List;

public class BodyHandler<T> {
    private final Class<T> bodyClass;
    private final Validator<T> validator;

    public BodyHandler(final Class<T> bodyClass, final Validator<T> validator) {
        this.bodyClass = bodyClass;
        this.validator = validator;
    }

    public T get(final Context context) {
        return RestJsonMapper.asClass(context.body(), bodyClass);
    }

    public T getValidated(final Context context) {
        T body = RestJsonMapper.asClass(context.body(), bodyClass);

        if (DomainScoped.class.isAssignableFrom(body.getClass())) {
            EntityDomainChecker.checkEntityDomainOrFail((DomainScoped) body, context);
        }

        List<Violation> violations = validator.validate(body);

        if (!violations.isEmpty()) {
            throw new RequestValidationException(violations);
        }

        return body;
    }

    public static class Builder<T> {
        private Class<T> bodyClass;

        public Builder(final Class<T> bodyClass) {
            this.bodyClass = bodyClass;
        }

        public Builder<T> bodyClass(final Class<T> bodyClass) {
            this.bodyClass = bodyClass;
            return this;
        }

        public BodyHandler<T> build() {
            Validator<T> validator = Validators.getForClass(bodyClass);

            if (validator == null) {
                throw new IllegalStateException("No validator was found for class " + bodyClass);
            }

            return new BodyHandler<>(bodyClass, validator);
        }
    }
}
