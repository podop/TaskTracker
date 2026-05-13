package com.tasktracker.controller;

import com.tasktracker.dto.TicketResponse;
import com.tasktracker.service.TicketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/manager/tickets")
public class ManagerTicketController {

    private final TicketService tickets;

    public ManagerTicketController(TicketService tickets) {
        this.tickets = tickets;
    }

    @GetMapping("/done-candidates")
    public List<TicketResponse> doneCandidates() {
        return tickets.listDoneCandidates().stream().map(TicketResponse::from).toList();
    }

    @PatchMapping("/{id}/mark-done")
    public TicketResponse markDone(@PathVariable("id") UUID id) {
        return TicketResponse.from(tickets.markDone(id));
    }
}
