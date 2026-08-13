package com.minidoodle.shared.mapper;

import com.minidoodle.shared.persistence.entity.MeetingEntity;
import com.minidoodle.shared.persistence.entity.MeetingParticipantEntity;
import com.minidoodle.shared.domain.Meeting;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper component for converting between MeetingEntity and Meeting domain object.
 * Ensures the service layer never deals with entities directly.
 */
@Component
public class MeetingMapper {

    /**
     * Converts a MeetingEntity to a Meeting domain object.
     *
     * @param entity the entity to convert
     * @return the domain object, or null if entity is null
     */
    public Meeting toDomain(MeetingEntity entity) {
        if (entity == null) {
            return null;
        }
        List<String> participants = entity.getParticipants().stream()
                .map(MeetingParticipantEntity::getUsername)
                .collect(Collectors.toList());

        return new Meeting(
                entity.getId(),
                entity.getTitle(),
                entity.getDescription(),
                participants,
                entity.getStartTime(),
                entity.getEndTime()
        );
    }

    /**
     * Converts a Meeting domain object to a MeetingEntity.
     *
     * @param meeting the domain object to convert
     * @return the entity, or null if meeting is null
     */
    public MeetingEntity toEntity(Meeting meeting) {
        if (meeting == null) {
            return null;
        }
        MeetingEntity entity = new MeetingEntity();
        entity.setId(meeting.getId());
        entity.setTitle(meeting.getTitle());
        entity.setDescription(meeting.getDescription());
        entity.setStartTime(meeting.getStart());
        entity.setEndTime(meeting.getEnd());

        if (meeting.getParticipants() != null) {
            for (String username : meeting.getParticipants()) {
                entity.addParticipant(username);
            }
        }

        return entity;
    }

    /**
     * Converts a list of MeetingEntity objects to Meeting domain objects.
     *
     * @param entities the entities to convert
     * @return list of domain objects
     */
    public List<Meeting> toDomainList(List<MeetingEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Converts a list of Meeting domain objects to MeetingEntity objects.
     *
     * @param meetings the domain objects to convert
     * @return list of entities
     */
    public List<MeetingEntity> toEntityList(List<Meeting> meetings) {
        if (meetings == null) {
            return List.of();
        }
        return meetings.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}