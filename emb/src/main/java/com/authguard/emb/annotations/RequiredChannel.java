package com.authguard.emb.annotations;

import java.lang.annotation.Annotation;

public class RequiredChannel implements Channel {
    private final String value;

    public RequiredChannel(final String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return this.value;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return Channel.class;
    }
}
