package com.minidoodle.scheduler.graphql;

import com.minidoodle.shared.constants.ErrorCode;
import com.minidoodle.shared.exceptions.SchedulingException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves scheduling-domain exceptions thrown while executing data fetchers
 * into typed GraphQL errors. Never leaks stack traces — only the domain
 * message and a stable {@code errorCode} extension are exposed.
 */
@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof ConstraintViolationException) {
            return GraphqlErrorBuilder.newError()
                    .message(ex.getMessage())
                    .errorType(ErrorType.BAD_REQUEST)
                    .extensions(Map.of("errorCode", "INVALID_INPUT"))
                    .build();
        }
        if (ex instanceof SchedulingException schedulingException) {
            return error(schedulingException.getErrorCode(), schedulingException.getMessage(),
                    errorType(schedulingException.getErrorCode()));
        }
        return null;
    }

    private GraphQLError error(ErrorCode errorCode, String message, ErrorType errorType) {
        return GraphqlErrorBuilder.newError()
                .message(message)
                .errorType(errorType)
                .extensions(Map.of("errorCode", errorCode.name()))
                .build();
    }

    private ErrorType errorType(ErrorCode errorCode) {
        return switch (errorCode) {
            case SLOT_NOT_FOUND, MEETING_NOT_FOUND -> ErrorType.NOT_FOUND;
            case INVALID_TIME_RANGE, INVALID_PARTICIPANTS -> ErrorType.BAD_REQUEST;
            case SLOT_CONFLICT, SLOT_LINKED_TO_MEETING -> ErrorType.FORBIDDEN;
            default -> ErrorType.INTERNAL_ERROR;
        };
    }
}