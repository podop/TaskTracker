package com.tasktracker.dto;

import com.tasktracker.model.Ticket;
import com.tasktracker.model.TicketStatus;

import java.time.Instant;
import java.util.UUID;

public record TicketResponse(
        UUID id,
        String title,
        String description,
        TicketStatus status,
        String assignee,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getAssigneeId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
