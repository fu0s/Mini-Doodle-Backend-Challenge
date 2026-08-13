package com.minidoodle.shared.persistence.repository;

import com.minidoodle.shared.persistence.entity.SlotEntity;
import com.minidoodle.shared.constants.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA repository for SlotEntity.
 * Provides indexed queries by username, time range, and meetingId.
 */
@Repository
public interface SlotRepository extends JpaRepository<SlotEntity, Long> {

    /**
     * Finds all slots for a specific user.
     *
     * @param username the username to query
     * @return list of slots
     */
    List<SlotEntity> findByUsername(String username);

    /**
     * Finds slots for a set of users.
     * Used by the slotsByUsername DataLoader for batched resolution.
     *
     * @param usernames the usernames to query
     * @return list of slots owned by any of the given users
     */
    List<SlotEntity> findByUsernameIn(Collection<String> usernames);

    /**
     * Finds slots for a user within a time range.
     *
     * @param username the username
     * @param start start of time range (inclusive)
     * @param end end of time range (exclusive)
     * @return list of slots
     */
    List<SlotEntity> findByUsernameAndStartTimeBetween(String username, LocalDateTime start, LocalDateTime end);

    /**
     * Finds all slots linked to a specific meeting.
     *
     * @param meetingId the meeting ID
     * @return list of slots with the given meetingId
     */
    List<SlotEntity> findByMeetingId(Long meetingId);

    /**
     * Finds slots by username and status.
     *
     * @param username the username
     * @param status the slot status
     * @return list of slots
     */
    List<SlotEntity> findByUsernameAndStatus(String username, SlotStatus status);

    /**
     * Finds FREE slots for a user that fully cover a given time range.
     * Used to check availability for meeting creation.
     *
     * @param username the username
     * @param start meeting start time
     * @param end meeting end time
     * @return list of FREE slots that cover the requested range
     */
    @Query("SELECT s FROM SlotEntity s WHERE s.username = :username " +
            "AND s.status = 'FREE' " +
            "AND s.startTime <= :start " +
            "AND s.endTime >= :end")
    List<SlotEntity> findFreeSlotsCoveringRange(@Param("username") String username,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    /**
     * Deletes all slots linked to a specific meeting.
     *
     * @param meetingId the meeting ID
     * @return number of deleted slots
     */
    @Query("DELETE FROM SlotEntity s WHERE s.meetingId = :meetingId")
    int deleteByMeetingId(@Param("meetingId") Long meetingId);

    /**
     * Finds BUSY slots linked to a meeting for a specific participant.
     *
     * @param username the username
     * @param meetingId the meeting ID
     * @return list of matching slots
     */
    List<SlotEntity> findByUsernameAndMeetingId(String username, Long meetingId);
}