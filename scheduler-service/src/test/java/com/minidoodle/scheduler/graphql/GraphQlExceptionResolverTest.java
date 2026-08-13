package com.minidoodle.scheduler.graphql;

import com.minidoodle.shared.exceptions.InvalidTimeRangeException;
import com.minidoodle.shared.exceptions.MeetingNotFoundException;
import com.minidoodle.shared.exceptions.SlotConflictException;
import com.minidoodle.shared.exceptions.SlotNotFoundException;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GraphQlExceptionResolverTest {

    private final GraphQlExceptionResolver resolver = new GraphQlExceptionResolver();
    private final DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);

    @Test
    void mapsNotFoundExceptionsToNotFound() {
        GraphQLError slotError = resolver.resolveToSingleError(new SlotNotFoundException(1L), env);
        assertThat(slotError.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(slotError.getExtensions()).containsEntry("errorCode", "SLOT_NOT_FOUND");

        GraphQLError meetingError = resolver.resolveToSingleError(new MeetingNotFoundException(2L), env);
        assertThat(meetingError.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(meetingError.getExtensions()).containsEntry("errorCode", "MEETING_NOT_FOUND");
    }

    @Test
    void mapsValidationExceptionsToBadRequest() {
        GraphQLError rangeError = resolver.resolveToSingleError(
                new InvalidTimeRangeException(LocalDateTime.now(), LocalDateTime.now().minusMinutes(5)), env);
        assertThat(rangeError.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(rangeError.getExtensions()).containsEntry("errorCode", "INVALID_TIME_RANGE");
    }

    @Test
    void mapsConflictExceptionsToForbidden() {
        GraphQLError conflictError = resolver.resolveToSingleError(
                new SlotConflictException("kuku", LocalDateTime.now(), LocalDateTime.now().plusHours(1)), env);
        assertThat(conflictError.getErrorType()).isEqualTo(ErrorType.FORBIDDEN);
        assertThat(conflictError.getExtensions()).containsEntry("errorCode", "SLOT_CONFLICT");
    }

    @Test
    void returnsNullForUnhandledExceptions() {
        assertThat(resolver.resolveToSingleError(new IllegalStateException("boom"), env)).isNull();
    }
}