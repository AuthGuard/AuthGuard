package com.nexblocks.authguard.service.model;

import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS) // Make it class retention for incremental compilation
@Value.Style(
        get = {"is*", "get*"}, // Detect 'get' and 'is' prefixes in accessor methods
        typeImmutable = "*BO", // Suffix with BO
        jdkOnly = true, // Prevent the use of Guava's collections, Mapstruct doesn't like them
        validationMethod = Value.Style.ValidationMethod.NONE,
        unsafeDefaultAndDerived = true
)
@interface BOStyle {
}
