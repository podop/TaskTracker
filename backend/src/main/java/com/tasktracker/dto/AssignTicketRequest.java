package com.tasktracker.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignTicketRequest(
        @NotBlank String assignee
) {
}
