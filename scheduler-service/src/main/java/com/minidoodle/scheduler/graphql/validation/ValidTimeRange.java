package com.minidoodle.scheduler.graphql.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level constraint asserting that an input's {@code start} is before its
 * {@code end}. Applies to records exposing {@code start()} and {@code end()}
 * accessors.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidTimeRangeValidator.class)
@Documented
public @interface ValidTimeRange {

    /**
     * Message rendered when the time range is invalid.
     */
    String message() default "end time must be after start time";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}