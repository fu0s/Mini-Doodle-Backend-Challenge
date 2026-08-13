package com.minidoodle.shared.mapper;

import com.minidoodle.shared.domain.Meeting;
import com.minidoodle.shared.persistence.entity.MeetingEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link MeetingMapper}.
 * Verifies bidirectional conversion between {@link MeetingEntity} and
 * {@link Meeting}, including participant mapping.
 */
class MeetingMapperTest {

    private final MeetingMapper mapper = new MeetingMapper();

    private final LocalDateTime start = LocalDateTime.of(2026, 1, 1, 9, 0);
    private final LocalDateTime end = start.plusHours(1);

    private MeetingEntity buildEntityWithParticipants(Long id, List<String> usernames) {
        MeetingEntity entity = new MeetingEntity(id, "Sync", "desc", start, end);
        for (String username : usernames) {
            entity.addParticipant(username);
        }
        return entity;
    }

    @Test
    void toDomain_mapsAllFieldsAndParticipants() {
        MeetingEntity entity = buildEntityWithParticipants(3L, List.of("alice", "bob"));

        Meeting meeting = mapper.toDomain(entity);

        assertEquals(3L, meeting.getId());
        assertEquals("Sync", meeting.getTitle());
        assertEquals("desc", meeting.getDescription());
        assertEquals(start, meeting.getStart());
        assertEquals(end, meeting.getEnd());
        assertEquals(List.of("alice", "bob"), meeting.getParticipants());
    }

    @Test
    void toDomain_nullEntity_returnsNull() {
        assertNull(mapper.toDomain(null));
    }

    @Test
    void toEntity_mapsAllFieldsAndParticipants() {
        Meeting meeting = new Meeting(3L, "Sync", "desc", List.of("alice", "bob"), start, end);

        MeetingEntity entity = mapper.toEntity(meeting);

        assertEquals(3L, entity.getId());
        assertEquals("Sync", entity.getTitle());
        assertEquals(start, entity.getStartTime());
        assertEquals(end, entity.getEndTime());
        assertEquals(2, entity.getParticipants().size());
    }

    @Test
    void toEntity_nullMeeting_returnsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toEntity_nullParticipants_leavesEntityEmpty() {
        Meeting meeting = new Meeting(3L, "Sync", "desc", null, start, end);

        MeetingEntity entity = mapper.toEntity(meeting);

        assertEquals(0, entity.getParticipants().size());
    }

    @Test
    void toDomainList_mapsEachElement() {
        List<MeetingEntity> entities = List.of(
                buildEntityWithParticipants(1L, List.of("alice")),
                buildEntityWithParticipants(2L, List.of("bob", "carl")));

        List<Meeting> meetings = mapper.toDomainList(entities);

        assertEquals(2, meetings.size());
        assertEquals(List.of("alice"), meetings.get(0).getParticipants());
        assertEquals(List.of("bob", "carl"), meetings.get(1).getParticipants());
    }

    @Test
    void toDomainList_nullList_returnsEmpty() {
        assertEquals(0, mapper.toDomainList(null).size());
    }

    @Test
    void toEntityList_mapsEachElement() {
        List<Meeting> meetings = List.of(
                new Meeting(1L, "Sync", "desc", List.of("alice"), start, end));

        List<MeetingEntity> entities = mapper.toEntityList(meetings);

        assertEquals(1, entities.size());
        assertEquals("Sync", entities.get(0).getTitle());
        assertEquals(1, entities.get(0).getParticipants().size());
    }

    @Test
    void toEntityList_nullList_returnsEmpty() {
        assertEquals(0, mapper.toEntityList(null).size());
    }
}