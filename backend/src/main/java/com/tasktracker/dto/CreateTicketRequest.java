package com.tasktracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description
) {
}
