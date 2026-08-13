package com.minidoodle.shared.service;

import com.minidoodle.shared.domain.Meeting;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Service interface for meeting management operations.
 * Implementations handle meeting creation, updates, deletion, and queries.
 * Meeting operations automatically manage participant slots via Kafka events.
 */
public interface MeetingService {
    
    /**
     * Creates a new meeting and publishes a CREATED event to Kafka.
     * The consumer service will automatically create BUSY slots for all participants.
     *
     * @param title meeting title
     * @param description meeting description
     * @param participants list of participant usernames
     * @param start meeting start time
     * @param end meeting end time
     * @return the created meeting
     * @throws com.minidoodle.shared.exceptions.InvalidTimeRangeException if end <= start
     * @throws com.minidoodle.shared.exceptions.InvalidParticipantsException if participants list is invalid
     * @throws com.minidoodle.shared.exceptions.SlotConflictException if any participant is not available
     */
    Meeting createMeeting(String title, String description, List<String> participants,
                         LocalDateTime start, LocalDateTime end);
    
    /**
     * Updates an existing meeting and publishes an UPDATED event to Kafka.
     * The consumer service will update participant slots accordingly.
     *
     * @param meetingId the ID of the meeting to update
     * @param title new meeting title
     * @param description new meeting description
     * @param participants new list of participant usernames
     * @param start new meeting start time
     * @param end new meeting end time
     * @return the updated meeting
     * @throws com.minidoodle.shared.exceptions.MeetingNotFoundException if meeting not found
     * @throws com.minidoodle.shared.exceptions.InvalidTimeRangeException if end <= start
     * @throws com.minidoodle.shared.exceptions.InvalidParticipantsException if participants list is invalid
     * @throws com.minidoodle.shared.exceptions.SlotConflictException if any participant is not available
     */
    Meeting updateMeeting(Long meetingId, String title, String description, List<String> participants,
                         LocalDateTime start, LocalDateTime end);
    
    /**
     * Deletes a meeting and publishes a DELETED event to Kafka.
     * The consumer service will free all linked participant slots.
     *
     * @param meetingId the ID of the meeting to delete
     * @throws com.minidoodle.shared.exceptions.MeetingNotFoundException if meeting not found
     */
    void deleteMeeting(Long meetingId);
    
    /**
     * Retrieves all meetings involving a specific user as a participant.
     *
     * @param username the username to query
     * @return list of meetings involving the user
     */
    List<Meeting> getMeetingsByUsername(String username);

    /**
     * Retrieves multiple meetings by their IDs in a single batched query.
     * Used by DataLoaders to prevent N+1 queries.
     *
     * @param meetingIds the meeting IDs to query
     * @return list of meetings matching the given IDs
     */
    List<Meeting> getMeetingsByIds(Collection<Long> meetingIds);
    
    /**
     * Retrieves a meeting by its ID.
     *
     * @param meetingId the meeting ID
     * @return the meeting
     * @throws com.minidoodle.shared.exceptions.MeetingNotFoundException if meeting not found
     */
    Meeting getMeetingById(Long meetingId);
}
