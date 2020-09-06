package com.authguard.api.dto.validation.fluent;

import com.authguard.api.dto.validation.Validator;

import java.util.function.Supplier;

public class ValidationTarget<T> {
    private final String fieldName;
    private final Supplier<T> fieldValueSupplier;
    private final Validator<T> validator;

    public ValidationTarget(final String fieldName, final Supplier<T> fieldValueSupplier, final Validator<T> validator) {
        this.fieldName = fieldName;
        this.fieldValueSupplier = fieldValueSupplier;
        this.validator = validator;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Supplier<T> getFieldValueSupplier() {
        return fieldValueSupplier;
    }

    public Validator<T> getValidator() {
        return validator;
    }
}
