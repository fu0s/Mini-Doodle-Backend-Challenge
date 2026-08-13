package com.minidoodle.shared.persistence.repository;

import com.minidoodle.shared.persistence.entity.MeetingEntity;
import com.minidoodle.shared.persistence.entity.MeetingParticipantEntity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest}
 * validating {@link MeetingParticipantRepository} composite-key lookups.
 */
@Tag("integration")
@DataJpaTest
class MeetingParticipantRepositoryIntegrationTest {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MeetingParticipantRepository participantRepository;

    private final LocalDateTime t9 = LocalDateTime.of(2026, 8, 15, 9, 0);
    private final LocalDateTime t10 = LocalDateTime.of(2026, 8, 15, 10, 0);

    private MeetingEntity saveMeeting(String username) {
        MeetingEntity meeting = new MeetingEntity(null, "Standup", "daily", t9, t10);
        meeting.addParticipant(username);
        return meetingRepository.save(meeting);
    }

    @Test
    void findByMeeting_Id_ReturnsParticipantsForMeeting() {
        MeetingEntity meeting = saveMeeting("alice");
        List<MeetingParticipantEntity> participants =
                participantRepository.findByMeeting_Id(meeting.getId());
        assertEquals(1, participants.size());
        assertEquals("alice", participants.get(0).getUsername());
    }

    @Test
    void findByUsername_ReturnsParticipantsAcrossMeetings() {
        MeetingEntity m1 = saveMeeting("alice");
        MeetingEntity m2 = saveMeeting("alice");
        List<MeetingParticipantEntity> alice = participantRepository.findByUsername("alice");
        assertEquals(2, alice.size());
    }

    @Test
    void isParticipant_ReturnsTrueWhenUserLinked() {
        MeetingEntity meeting = saveMeeting("bob");
        assertTrue(participantRepository.isParticipant(meeting.getId(), "bob"));
        assertFalse(participantRepository.isParticipant(meeting.getId(), "alice"));
    }
}