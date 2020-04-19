package com.authguard.emb.model;

import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
@Value.Style(
        get = {"is*", "get*"},
        typeAbstract = "*Interface",
        typeImmutable = "*",
        validationMethod = Value.Style.ValidationMethod.NONE,
        jdkOnly = true
)
@interface MOStyle {
}
