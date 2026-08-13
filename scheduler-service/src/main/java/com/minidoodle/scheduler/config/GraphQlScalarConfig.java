package com.minidoodle.scheduler.config;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.NullValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Registers the custom {@code DateTime} scalar from the GraphQL schema.
 * Spring for GraphQL provides no built-in datetime scalar, so the schema's
 * {@code scalar DateTime} has to be wired explicitly or schema construction
 * fails at application startup.
 */
@Configuration
public class GraphQlScalarConfig {

    /**
     * {@code DateTime} scalar backed by {@link LocalDateTime}, serialized as
     * ISO-8601 strings. Custom exceptions are never surfaced as booleans or
     * magic strings; parse failures throw the documented coercion exceptions.
     */
    @Bean
    GraphQLScalarType dateTimeScalar() {
        Coercing<LocalDateTime, String> coercing = new Coercing<LocalDateTime, String>() {

            @Override
            public String serialize(Object dataFetcherResult, GraphQLContext graphQLContext, Locale locale)
                    throws CoercingSerializeException {
                if (dataFetcherResult == null) {
                    return null;
                }
                if (dataFetcherResult instanceof LocalDateTime dateTime) {
                    return dateTime.toString();
                }
                if (dataFetcherResult instanceof String text) {
                    return text;
                }
                throw new CoercingSerializeException("Expected a LocalDateTime but got: " +
                        dataFetcherResult.getClass().getSimpleName());
            }

            @Override
            public LocalDateTime parseValue(Object input, GraphQLContext graphQLContext, Locale locale)
                    throws CoercingParseValueException {
                if (input instanceof String text) {
                    return parseDateTime(text);
                }
                if (input instanceof LocalDateTime dateTime) {
                    return dateTime;
                }
                throw new CoercingParseValueException("Expected an ISO-8601 string but got: " +
                        input.getClass().getSimpleName());
            }

            @Override
            public LocalDateTime parseLiteral(Value<?> input, CoercedVariables variables,
                                              GraphQLContext graphQLContext, Locale locale)
                    throws CoercingParseLiteralException {
                if (input instanceof NullValue) {
                    return null;
                }
                if (input instanceof StringValue stringValue) {
                    return parseDateTime(stringValue.getValue());
                }
                throw new CoercingParseLiteralException("Expected an ISO-8601 string literal");
            }

            private LocalDateTime parseDateTime(String text) {
                try {
                    return LocalDateTime.parse(text);
                } catch (DateTimeParseException ex) {
                    throw new CoercingParseValueException("Invalid date/time: " + text, ex);
                }
            }
        };

        return GraphQLScalarType.newScalar()
                .name("DateTime")
                .coercing(coercing)
                .build();
    }

    /**
     * Wires the {@link #dateTimeScalar()} into the schema's runtime wiring.
     */
    @Bean
    RuntimeWiringConfigurer dateTimeScalarWiringConfigurer(GraphQLScalarType dateTimeScalar) {
        return builder -> builder.scalar(dateTimeScalar);
    }
}