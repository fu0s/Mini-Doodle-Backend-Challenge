package com.minidoodle.shared.persistence.repository;

import com.minidoodle.shared.persistence.entity.MeetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for MeetingEntity.
 * Provides indexed queries by participant and time range.
 */
@Repository
public interface MeetingRepository extends JpaRepository<MeetingEntity, Long> {

    /**
     * Finds all meetings where a user is a participant.
     *
     * @param username the participant username
     * @return list of meetings
     */
    @Query("SELECT DISTINCT m FROM MeetingEntity m JOIN m.participants p WHERE p.username = :username")
    List<MeetingEntity> findByParticipantUsername(@Param("username") String username);

    /**
     * Finds meetings for a participant within a time range.
     *
     * @param username the participant username
     * @param start start of time range
     * @param end end of time range
     * @return list of meetings
     */
    @Query("SELECT DISTINCT m FROM MeetingEntity m JOIN m.participants p " +
            "WHERE p.username = :username " +
            "AND m.startTime < :end AND m.endTime > :start")
    List<MeetingEntity> findByParticipantUsernameAndTimeRange(@Param("username") String username,
                                                               @Param("start") LocalDateTime start,
                                                               @Param("end") LocalDateTime end);

    /**
     * Finds meetings by exact time range and participant.
     *
     * @param username the participant username
     * @param start meeting start time
     * @param end meeting end time
     * @return matching meetings
     */
    @Query("SELECT DISTINCT m FROM MeetingEntity m JOIN m.participants p " +
            "WHERE p.username = :username " +
            "AND m.startTime = :start " +
            "AND m.endTime = :end")
    List<MeetingEntity> findByParticipantUsernameAndExactTime(@Param("username") String username,
                                                               @Param("start") LocalDateTime start,
                                                               @Param("end") LocalDateTime end);
}