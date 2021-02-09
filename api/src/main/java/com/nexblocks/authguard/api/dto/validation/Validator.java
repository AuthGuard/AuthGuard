package com.nexblocks.authguard.api.dto.validation;

import com.nexblocks.authguard.api.dto.validation.violations.Violation;

import java.util.List;

@FunctionalInterface
public interface Validator<T> {
    List<Violation> validate(T obj);

    default List<Violation> validate(T obj, String withFieldName) {
        return validate(obj);
    }
}
