package com.minidoodle.shared.persistence.repository;

import com.minidoodle.shared.persistence.entity.MeetingParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for MeetingParticipantEntity.
 * Provides queries for participant lookups.
 */
@Repository
public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipantEntity, com.minidoodle.shared.persistence.entity.MeetingParticipantId> {

    /**
     * Finds all participants for a meeting.
     *
     * @param meetingId the meeting ID
     * @return list of participants
     */
    List<MeetingParticipantEntity> findByMeeting_Id(Long meetingId);

    /**
     * Finds meetings for a specific participant.
     *
     * @param username the participant username
     * @return list of meeting-participant entries
     */
    List<MeetingParticipantEntity> findByUsername(String username);

    /**
     * Checks if a user is a participant in a meeting.
     *
     * @param meetingId the meeting ID
     * @param username the username
     * @return true if user is a participant
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM MeetingParticipantEntity p " +
            "WHERE p.meeting.id = :meetingId AND p.username = :username")
    boolean isParticipant(@Param("meetingId") Long meetingId, @Param("username") String username);
}