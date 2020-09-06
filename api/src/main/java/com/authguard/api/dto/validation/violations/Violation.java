package com.authguard.api.dto.validation.violations;

import java.util.Objects;

public class Violation {
    private String field;
    private ViolationType type;

    public Violation(final ViolationType type) {
        this.type = type;
    }

    public Violation(final String field, final ViolationType type) {
        this.field = field;
        this.type = type;
    }

    public String getField() {
        return field;
    }

    public ViolationType getType() {
        return type;
    }

    public Violation setField(final String field) {
        this.field = field;
        return this;
    }

    public Violation setType(final ViolationType type) {
        this.type = type;
        return this;
    }

    @Override
    public String toString() {
        return "Violation{" +
                "field='" + field + '\'' +
                ", type=" + type +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Violation violation = (Violation) o;
        return Objects.equals(field, violation.field) &&
                type == violation.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, type);
    }
}
