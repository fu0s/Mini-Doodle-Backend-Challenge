package com.minidoodle.shared.mapper;

import com.minidoodle.shared.constants.SlotStatus;
import com.minidoodle.shared.domain.Slot;
import com.minidoodle.shared.persistence.entity.SlotEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link SlotMapper}.
 * Verifies bidirectional conversion between {@link SlotEntity} and {@link Slot},
 * including the meetingId linkage field.
 */
class SlotMapperTest {

    private final SlotMapper mapper = new SlotMapper();

    private final LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
    private final LocalDateTime end = start.plusHours(2);

    @Test
    void toDomain_mapsAllFieldsIncludingMeetingId() {
        SlotEntity entity = new SlotEntity(7L, "alice", start, end, SlotStatus.BUSY, 42L);

        Slot slot = mapper.toDomain(entity);

        assertEquals(7L, slot.getId());
        assertEquals("alice", slot.getUsername());
        assertEquals(start, slot.getStart());
        assertEquals(end, slot.getEnd());
        assertEquals(SlotStatus.BUSY, slot.getStatus());
        assertEquals(42L, slot.getMeetingId());
        assertEquals(true, slot.isLinkedToMeeting());
    }

    @Test
    void toDomain_nullEntity_returnsNull() {
        assertNull(mapper.toDomain(null));
    }

    @Test
    void toDomain_freeSlot_meetingIdNull() {
        SlotEntity entity = new SlotEntity(7L, "alice", start, end, SlotStatus.FREE, null);

        Slot slot = mapper.toDomain(entity);

        assertNull(slot.getMeetingId());
        assertEquals(false, slot.isLinkedToMeeting());
    }

    @Test
    void toEntity_mapsAllFieldsBack() {
        Slot slot = new Slot(7L, "alice", start, end, SlotStatus.BUSY, 42L);

        SlotEntity entity = mapper.toEntity(slot);

        assertEquals(7L, entity.getId());
        assertEquals("alice", entity.getUsername());
        assertEquals(start, entity.getStartTime());
        assertEquals(end, entity.getEndTime());
        assertEquals(SlotStatus.BUSY, entity.getStatus());
        assertEquals(42L, entity.getMeetingId());
    }

    @Test
    void toEntity_nullSlot_returnsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toDomainList_mapsEachElement() {
        List<SlotEntity> entities = List.of(
                new SlotEntity(1L, "alice", start, end, SlotStatus.FREE, null),
                new SlotEntity(2L, "alice", end, end.plusHours(1), SlotStatus.BUSY, 9L));

        List<Slot> slots = mapper.toDomainList(entities);

        assertEquals(2, slots.size());
        assertEquals(1L, slots.get(0).getId());
        assertEquals(9L, slots.get(1).getMeetingId());
    }

    @Test
    void toDomainList_nullList_returnsEmpty() {
        assertEquals(0, mapper.toDomainList(null).size());
    }

    @Test
    void toEntityList_mapsEachElement() {
        List<Slot> slots = List.of(
                new Slot(1L, "alice", start, end, SlotStatus.FREE, null),
                new Slot(2L, "alice", end, end.plusHours(1), SlotStatus.BUSY, 9L));

        List<SlotEntity> entities = mapper.toEntityList(slots);

        assertEquals(2, entities.size());
        assertEquals(SlotStatus.FREE, entities.get(0).getStatus());
        assertEquals(9L, entities.get(1).getMeetingId());
    }

    @Test
    void toEntityList_nullList_returnsEmpty() {
        assertEquals(0, mapper.toEntityList(null).size());
    }
}