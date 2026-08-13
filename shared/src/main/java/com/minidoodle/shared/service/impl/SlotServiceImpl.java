package com.minidoodle.shared.service.impl;

import com.minidoodle.shared.constants.SlotStatus;
import com.minidoodle.shared.domain.Slot;
import com.minidoodle.shared.exceptions.InvalidTimeRangeException;
import com.minidoodle.shared.exceptions.SlotLinkedToMeetingException;
import com.minidoodle.shared.exceptions.SlotNotFoundException;
import com.minidoodle.shared.mapper.SlotMapper;
import com.minidoodle.shared.persistence.entity.SlotEntity;
import com.minidoodle.shared.persistence.repository.SlotRepository;
import com.minidoodle.shared.service.SlotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Implementation of SlotService for slot management operations.
 * Handles slot creation, updates, deletion, and queries with proper business rule validation.
 */
@Service
public class SlotServiceImpl implements SlotService {

    private final SlotRepository slotRepository;
    private final SlotMapper slotMapper;

    public SlotServiceImpl(SlotRepository slotRepository, SlotMapper slotMapper) {
        this.slotRepository = slotRepository;
        this.slotMapper = slotMapper;
    }

    @Override
    @Transactional
    public Slot createSlot(String username, LocalDateTime start, LocalDateTime end, SlotStatus status) {
        validateTimeRange(start, end);
        
        SlotStatus resolvedStatus = (status != null) ? status : SlotStatus.FREE;
        
        SlotEntity entity = new SlotEntity();
        entity.setUsername(username);
        entity.setStartTime(start);
        entity.setEndTime(end);
        entity.setStatus(resolvedStatus);
        entity.setMeetingId(null);  // No meetingId on manual creation
        
        SlotEntity saved = slotRepository.save(entity);
        return slotMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public Slot updateSlot(Long slotId, LocalDateTime start, LocalDateTime end) {
        validateTimeRange(start, end);
        
        SlotEntity slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException(slotId));
        
        // Guard check: cannot update slot linked to a meeting
        guardSlotLinkedToMeeting(slot);
        
        slot.setStartTime(start);
        slot.setEndTime(end);
        
        SlotEntity saved = slotRepository.save(slot);
        return slotMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteSlot(Long slotId) {
        SlotEntity slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException(slotId));
        
        // Guard check: cannot delete slot linked to a meeting
        guardSlotLinkedToMeeting(slot);
        
        slotRepository.delete(slot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Slot> getSlotsByUsername(String username) {
        List<SlotEntity> entities = slotRepository.findByUsername(username);
        return slotMapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Slot> getSlotsByUsernames(Collection<String> usernames) {
        List<SlotEntity> entities = slotRepository.findByUsernameIn(usernames);
        return slotMapper.toDomainList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Slot> getSlotsByUsernameAndTimeRange(String username, LocalDateTime start, LocalDateTime end) {
        List<SlotEntity> entities = slotRepository.findByUsernameAndStartTimeBetween(username, start, end);
        return slotMapper.toDomainList(entities);
    }
    
    /**
     * Validates that the time range is valid (end must be after start).
     *
     * @param start start time
     * @param end end time
     * @throws InvalidTimeRangeException if end <= start
     */
    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (end == null || start == null || !end.isAfter(start)) {
            throw new InvalidTimeRangeException(start, end);
        }
    }
    
    /**
     * Guard check for slots linked to meetings.
     * If slot is BUSY and has a meetingId, throw SlotLinkedToMeetingException.
     *
     * @param slot the slot to check
     * @throws SlotLinkedToMeetingException if slot is linked to a meeting
     */
    private void guardSlotLinkedToMeeting(SlotEntity slot) {
        if (slot.getStatus() == SlotStatus.BUSY && slot.getMeetingId() != null) {
            throw new SlotLinkedToMeetingException(slot.getId(), slot.getMeetingId());
        }
    }
}
