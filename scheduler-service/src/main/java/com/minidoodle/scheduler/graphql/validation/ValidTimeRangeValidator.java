package com.minidoodle.scheduler.graphql.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

/**
 * Validates a {@link ValidTimeRange}-annotated input by comparing its
 * {@code start} and {@code end} accessors.
 */
public class ValidTimeRangeValidator implements ConstraintValidator<ValidTimeRange, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDateTime start = timeOf(value, "start");
        LocalDateTime end = timeOf(value, "end");
        if (start == null || end == null) {
            // Nullability is enforced by @NotNull on the individual fields.
            return true;
        }
        return end.isAfter(start);
    }

    private LocalDateTime timeOf(Object bean, String accessor) {
        try {
            return (LocalDateTime) bean.getClass().getMethod(accessor).invoke(bean);
        } catch (ReflectiveOperationException | ClassCastException ex) {
            return null;
        }
    }
}