package com.minidoodle.shared.persistence.repository;

import com.minidoodle.shared.persistence.entity.MeetingEntity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest}
 * validating {@link MeetingRepository} queries (participant join lookups).
 */
@Tag("integration")
@DataJpaTest
class MeetingRepositoryIntegrationTest {

    @Autowired
    private MeetingRepository meetingRepository;

    private final LocalDateTime t9 = LocalDateTime.of(2026, 8, 15, 9, 0);
    private final LocalDateTime t10 = LocalDateTime.of(2026, 8, 15, 10, 0);
    private final LocalDateTime t11 = LocalDateTime.of(2026, 8, 15, 11, 0);
    private final LocalDateTime t12 = LocalDateTime.of(2026, 8, 15, 12, 0);

    private MeetingEntity save(String title, String description, LocalDateTime start, LocalDateTime end,
                               String... usernames) {
        MeetingEntity meeting = new MeetingEntity(null, title, description, start, end);
        for (String username : usernames) {
            meeting.addParticipant(username);
        }
        return meetingRepository.save(meeting);
    }

    @Test
    void saveAndFindById_CascadesParticipants() {
        MeetingEntity saved = save("Standup", "daily", t9, t10, "alice", "bob");
        Optional<MeetingEntity> found = meetingRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Standup", found.get().getTitle());
        assertEquals(2, found.get().getParticipants().size());
        assertTrue(found.get().getParticipants().stream()
                .anyMatch(p -> "alice".equals(p.getUsername())));
        assertTrue(found.get().getParticipants().stream()
                .anyMatch(p -> "bob".equals(p.getUsername())));
    }

    @Test
    void findByParticipantUsername_ReturnsAllMeetingsForUser() {
        save("M1", "d1", t9, t10, "alice", "bob");
        save("M2", "d2", t10, t11, "bob", "carol");
        save("M3", "d3", t11, t12, "carol");
        List<MeetingEntity> bob = meetingRepository.findByParticipantUsername("bob");
        assertEquals(2, bob.size());
        assertEquals(2, bob.get(0).getParticipants().size());
        assertEquals("M1", bob.get(0).getTitle());
        assertEquals("M2", bob.get(1).getTitle());
    }

    @Test
    void findByParticipantUsername_ReturnsDistinctMeetings() {
        save("M1", "d1", t9, t12, "alice");
        List<MeetingEntity> result = meetingRepository.findByParticipantUsername("alice");
        assertEquals(1, result.size());
    }

    @Test
    void findByParticipantUsernameAndTimeRange_OverlapsAreMatched() {
        save("M1", "9-10", t9, t10, "alice");
        save("M2", "10-12", t10, t12, "alice");
        save("M3", "11-12", t11, t12, "bob");

        List<MeetingEntity> overlaps = meetingRepository
                .findByParticipantUsernameAndTimeRange("alice", t10, t12);
        assertEquals(1, overlaps.size());
        assertEquals("M2", overlaps.get(0).getTitle());

        List<MeetingEntity> crossBoundary = meetingRepository
                .findByParticipantUsernameAndTimeRange("alice", t9, t11);
        assertEquals(2, crossBoundary.size());
    }

    @Test
    void findByParticipantUsernameAndExactTime_MatchesExactRange() {
        save("M1", "d1", t9, t10, "alice");
        save("M2", "d2", t10, t11, "alice");
        List<MeetingEntity> exact = meetingRepository
                .findByParticipantUsernameAndExactTime("alice", t9, t10);
        assertEquals(1, exact.size());
        assertEquals("M1", exact.get(0).getTitle());
    }
}