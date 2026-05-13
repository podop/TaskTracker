package com.tasktracker.controller;

import com.tasktracker.dto.AssignTicketRequest;
import com.tasktracker.dto.CreateTicketRequest;
import com.tasktracker.dto.StatusChangeRequest;
import com.tasktracker.dto.TicketResponse;
import com.tasktracker.model.Ticket;
import com.tasktracker.model.TicketStatus;
import com.tasktracker.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService tickets;

    public TicketController(TicketService tickets) {
        this.tickets = tickets;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest req) {
        Ticket created = tickets.create(req);
        URI location = UriComponentsBuilder.fromPath("/api/tickets/{id}")
                .buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(TicketResponse.from(created));
    }

    @GetMapping
    public List<TicketResponse> list(@RequestParam(value = "status", required = false) TicketStatus status) {
        return tickets.list(status).stream().map(TicketResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable("id") UUID id) {
        return TicketResponse.from(tickets.get(id));
    }

    @PatchMapping("/{id}/assignee")
    public TicketResponse assign(@PathVariable("id") UUID id,
                                 @Valid @RequestBody AssignTicketRequest req) {
        return TicketResponse.from(tickets.assign(id, req.assignee()));
    }

    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(@PathVariable("id") UUID id,
                                       @Valid @RequestBody StatusChangeRequest req) {
        return TicketResponse.from(tickets.userTransition(id, req.status()));
    }
}
