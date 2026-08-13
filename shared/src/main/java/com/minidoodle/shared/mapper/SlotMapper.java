package com.minidoodle.shared.mapper;

import com.minidoodle.shared.persistence.entity.SlotEntity;
import com.minidoodle.shared.domain.Slot;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper component for converting between SlotEntity and Slot domain object.
 * Ensures the service layer never deals with entities directly.
 */
@Component
public class SlotMapper {

    /**
     * Converts a SlotEntity to a Slot domain object.
     *
     * @param entity the entity to convert
     * @return the domain object, or null if entity is null
     */
    public Slot toDomain(SlotEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Slot(
                entity.getId(),
                entity.getUsername(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStatus(),
                entity.getMeetingId()
        );
    }

    /**
     * Converts a Slot domain object to a SlotEntity.
     *
     * @param slot the domain object to convert
     * @return the entity, or null if slot is null
     */
    public SlotEntity toEntity(Slot slot) {
        if (slot == null) {
            return null;
        }
        SlotEntity entity = new SlotEntity();
        entity.setId(slot.getId());
        entity.setUsername(slot.getUsername());
        entity.setStartTime(slot.getStart());
        entity.setEndTime(slot.getEnd());
        entity.setStatus(slot.getStatus());
        entity.setMeetingId(slot.getMeetingId());
        return entity;
    }

    /**
     * Converts a list of SlotEntity objects to Slot domain objects.
     *
     * @param entities the entities to convert
     * @return list of domain objects
     */
    public List<Slot> toDomainList(List<SlotEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Converts a list of Slot domain objects to SlotEntity objects.
     *
     * @param slots the domain objects to convert
     * @return list of entities
     */
    public List<SlotEntity> toEntityList(List<Slot> slots) {
        if (slots == null) {
            return List.of();
        }
        return slots.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}