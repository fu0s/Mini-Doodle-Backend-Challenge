package com.minidoodle.shared.service.impl;

import com.minidoodle.shared.domain.Calendar;
import com.minidoodle.shared.domain.Meeting;
import com.minidoodle.shared.domain.Slot;
import com.minidoodle.shared.mapper.MeetingMapper;
import com.minidoodle.shared.mapper.SlotMapper;
import com.minidoodle.shared.persistence.entity.MeetingEntity;
import com.minidoodle.shared.persistence.entity.SlotEntity;
import com.minidoodle.shared.persistence.repository.MeetingRepository;
import com.minidoodle.shared.persistence.repository.SlotRepository;
import com.minidoodle.shared.service.CalendarService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of CalendarService for calendar view operations.
 * Provides aggregate views combining slots and meetings for a user.
 */
@Service
public class CalendarServiceImpl implements CalendarService {

    private final SlotRepository slotRepository;
    private final MeetingRepository meetingRepository;
    private final SlotMapper slotMapper;
    private final MeetingMapper meetingMapper;

    public CalendarServiceImpl(SlotRepository slotRepository,
                                MeetingRepository meetingRepository,
                                SlotMapper slotMapper,
                                MeetingMapper meetingMapper) {
        this.slotRepository = slotRepository;
        this.meetingRepository = meetingRepository;
        this.slotMapper = slotMapper;
        this.meetingMapper = meetingMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Calendar getCalendar(String username, LocalDateTime from, LocalDateTime to) {
        List<Slot> slots;
        List<Meeting> meetings;
        
        if (from != null && to != null) {
            // Filter by time range
            List<SlotEntity> slotEntities = slotRepository.findByUsernameAndStartTimeBetween(username, from, to);
            slots = slotMapper.toDomainList(slotEntities);
            
            List<MeetingEntity> meetingEntities = meetingRepository.findByParticipantUsernameAndTimeRange(username, from, to);
            meetings = meetingMapper.toDomainList(meetingEntities);
        } else {
            // No time filter - get all
            List<SlotEntity> slotEntities = slotRepository.findByUsername(username);
            slots = slotMapper.toDomainList(slotEntities);
            
            List<MeetingEntity> meetingEntities = meetingRepository.findByParticipantUsername(username);
            meetings = meetingMapper.toDomainList(meetingEntities);
        }
        
        return new Calendar(username, slots, meetings);
    }
}
