package com.tasktracker.dto;

import com.tasktracker.model.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(
        @NotNull TicketStatus status
) {
}
