package com.minidoodle.scheduler.graphql;

import com.minidoodle.scheduler.graphql.dto.CreateSlotInput;
import com.minidoodle.scheduler.graphql.dto.UpdateSlotInput;
import com.minidoodle.shared.domain.Slot;
import com.minidoodle.shared.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * GraphQL controller for slot queries and mutations.
 * Calls the {@link SlotService} interface only — never repositories.
 */
@Controller
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @QueryMapping
    public List<Slot> slotsByUsername(@Argument String username) {
        return slotService.getSlotsByUsername(username);
    }

    @MutationMapping
    public Slot createSlot(@Argument @Valid CreateSlotInput input) {
        return slotService.createSlot(input.username(), input.start(), input.end(), input.status());
    }

    @MutationMapping
    public Slot updateSlot(@Argument @Valid UpdateSlotInput input) {
        return slotService.updateSlot(input.slotId(), input.start(), input.end());
    }

    @MutationMapping
    public boolean deleteSlot(@Argument("slotId") Long slotId) {
        slotService.deleteSlot(slotId);
        return true;
    }
}