package com.minidoodle.shared.persistence.repository;

import com.minidoodle.shared.constants.SlotStatus;
import com.minidoodle.shared.persistence.entity.SlotEntity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest}
 * validating {@link SlotRepository} query behavior against a real database.
 */
@Tag("integration")
@DataJpaTest
class SlotRepositoryIntegrationTest {

    @Autowired
    private SlotRepository slotRepository;

    private final LocalDateTime t9 = LocalDateTime.of(2026, 8, 15, 9, 0);
    private final LocalDateTime t10 = LocalDateTime.of(2026, 8, 15, 10, 0);
    private final LocalDateTime t11 = LocalDateTime.of(2026, 8, 15, 11, 0);
    private final LocalDateTime t12 = LocalDateTime.of(2026, 8, 15, 12, 0);

    private SlotEntity save(String username, LocalDateTime start, LocalDateTime end,
                            SlotStatus status, Long meetingId) {
        SlotEntity entity = new SlotEntity(null, username, start, end, status, meetingId);
        return slotRepository.save(entity);
    }

    @Test
    void saveAndFindById_RoundTripsAllFields() {
        SlotEntity saved = save("alice", t9, t10, SlotStatus.BUSY, 7L);
        Optional<SlotEntity> found = slotRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("alice", found.get().getUsername());
        assertEquals(t9, found.get().getStartTime());
        assertEquals(t10, found.get().getEndTime());
        assertEquals(SlotStatus.BUSY, found.get().getStatus());
        assertEquals(7L, found.get().getMeetingId());
    }

    @Test
    void findByUsername_ReturnsOnlyMatching() {
        save("alice", t9, t10, SlotStatus.FREE, null);
        save("bob", t10, t11, SlotStatus.FREE, null);
        save("alice", t11, t12, SlotStatus.BUSY, 3L);
        List<SlotEntity> alice = slotRepository.findByUsername("alice");
        assertEquals(2, alice.size());
        assertTrue(alice.stream().allMatch(s -> "alice".equals(s.getUsername())));
    }

    @Test
    void findByUsernameIn_BatchesByUsernames() {
        save("alice", t9, t10, SlotStatus.FREE, null);
        save("bob", t10, t11, SlotStatus.FREE, null);
        save("carol", t11, t12, SlotStatus.FREE, null);
        List<SlotEntity> result = slotRepository.findByUsernameIn(List.of("alice", "bob"));
        assertEquals(2, result.size());
        assertTrue(result.stream().map(SlotEntity::getUsername)
                .allMatch(name -> Set.of("alice", "bob").contains(name)));
    }

    @Test
    void findByUsernameAndStartTimeBetween_IncludesBothBoundaries() {
        save("alice", t9, t10, SlotStatus.FREE, null);
        save("alice", t11, t12, SlotStatus.FREE, null);
        save("alice", t12, t10.plusHours(4), SlotStatus.FREE, null);
        List<SlotEntity> between = slotRepository
                .findByUsernameAndStartTimeBetween("alice", t9, t11);
        assertEquals(2, between.size());
        assertTrue(between.stream().anyMatch(s -> s.getStartTime().equals(t9)));
        assertTrue(between.stream().anyMatch(s -> s.getStartTime().equals(t11)));
    }

    @Test
    void findByMeetingId_ReturnsOnlyLinkedSlots() {
        save("alice", t9, t10, SlotStatus.BUSY, 5L);
        save("bob", t10, t11, SlotStatus.BUSY, 5L);
        save("carol", t11, t12, SlotStatus.FREE, null);
        List<SlotEntity> linked = slotRepository.findByMeetingId(5L);
        assertEquals(2, linked.size());
        assertTrue(linked.stream().allMatch(s -> s.getMeetingId() == 5L));
    }

    @Test
    void findByUsernameAndStatus_FiltersByStatus() {
        save("alice", t9, t10, SlotStatus.FREE, null);
        save("alice", t10, t11, SlotStatus.BUSY, 2L);
        save("alice", t11, t12, SlotStatus.FREE, null);
        List<SlotEntity> free = slotRepository.findByUsernameAndStatus("alice", SlotStatus.FREE);
        assertEquals(2, free.size());
        assertTrue(free.stream().allMatch(s -> s.getStatus() == SlotStatus.FREE));
    }

    @Test
    void findFreeSlotsCoveringRange_MatchesSlotsFullyCoveringRequest() {
        save("alice", t9, t12, SlotStatus.FREE, null);
        save("alice", t10, t11, SlotStatus.FREE, null);
        save("alice", t11, t12, SlotStatus.BUSY, 2L);
        save("bob", t9, t12, SlotStatus.FREE, null);
        List<SlotEntity> covering = slotRepository.findFreeSlotsCoveringRange("alice", t9, t11);
        assertEquals(1, covering.size());
        assertEquals(t9, covering.get(0).getStartTime());
        assertEquals(t12, covering.get(0).getEndTime());
    }

    @Test
    void findFreeSlotsCoveringRange_ExcludesBusyAndPartialSlots() {
        save("alice", t9, t10, SlotStatus.FREE, null);
        save("alice", t9, t10, SlotStatus.BUSY, 1L);
        List<SlotEntity> covering = slotRepository.findFreeSlotsCoveringRange("alice", t9, t10);
        assertEquals(1, covering.size());
        assertEquals(SlotStatus.FREE, covering.get(0).getStatus());
    }

    @Test
    void findByUsernameAndMeetingId_ReturnsMatchingPairs() {
        save("alice", t9, t10, SlotStatus.BUSY, 9L);
        save("alice", t10, t11, SlotStatus.BUSY, 8L);
        List<SlotEntity> linked = slotRepository.findByUsernameAndMeetingId("alice", 9L);
        assertEquals(1, linked.size());
        assertEquals(t9, linked.get(0).getStartTime());
    }

    @Test
    void deleteSlot_RemovesRow() {
        SlotEntity saved = save("alice", t9, t10, SlotStatus.FREE, null);
        slotRepository.delete(saved);
        assertFalse(slotRepository.findById(saved.getId()).isPresent());
    }
}