package com.nexblocks.authguard.service.config;

import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
@Value.Style(
        get = {"get*", "*"},
        jdkOnly = true,
        validationMethod = Value.Style.ValidationMethod.NONE,
        typeAbstract = "*Interface",
        typeImmutable = "*"
)
public @interface ConfigStyle {
}
