package com.tasktracker.service;

import com.tasktracker.dto.CreateTicketRequest;
import com.tasktracker.exception.IllegalTransitionException;
import com.tasktracker.exception.NotFoundException;
import com.tasktracker.exception.UnknownUserException;
import com.tasktracker.model.MockUser;
import com.tasktracker.model.Ticket;
import com.tasktracker.model.TicketStatus;
import com.tasktracker.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository tickets;
    private final MockUserService users;

    public TicketService(TicketRepository tickets, MockUserService users) {
        this.tickets = tickets;
        this.users = users;
    }

    @Transactional
    public Ticket create(CreateTicketRequest req) {
        Ticket t = new Ticket(req.title().trim(), req.description());
        return tickets.save(t);
    }

    @Transactional(readOnly = true)
    public List<Ticket> list(TicketStatus statusFilter) {
        return statusFilter == null
                ? tickets.findAllByOrderByUpdatedAtDesc()
                : tickets.findAllByStatusOrderByUpdatedAtDesc(statusFilter);
    }

    @Transactional(readOnly = true)
    public Ticket get(UUID id) {
        return tickets.findById(id).orElseThrow(() -> new NotFoundException("Ticket not found: " + id));
    }

    @Transactional
    public Ticket assign(UUID ticketId, String assigneeIdOrUsername) {
        MockUser user = users.findByIdOrUsername(assigneeIdOrUsername)
                .orElseThrow(() -> new UnknownUserException("Unknown mock user: " + assigneeIdOrUsername));
        Ticket t = get(ticketId);
        t.setAssigneeId(user.id());
        return t;
    }

    @Transactional
    public Ticket userTransition(UUID ticketId, TicketStatus next) {
        if (next == TicketStatus.DONE) {
            throw new IllegalTransitionException("Regular users cannot move tickets directly to DONE");
        }
        Ticket t = get(ticketId);
        TicketStatus current = t.getStatus();
        if (!current.canUserTransitionTo(next)) {
            throw new IllegalTransitionException(
                    "Illegal transition: " + current + " -> " + next);
        }
        t.setStatus(next);
        return t;
    }

    @Transactional
    public Ticket markDone(UUID ticketId) {
        Ticket t = get(ticketId);
        if (!t.getStatus().canManagerMarkDone()) {
            throw new IllegalTransitionException(
                    "Only DONE_CANDIDATE tickets can be marked DONE (current: " + t.getStatus() + ")");
        }
        t.setStatus(TicketStatus.DONE);
        return t;
    }

    @Transactional(readOnly = true)
    public List<Ticket> listDoneCandidates() {
        return tickets.findAllByStatusOrderByUpdatedAtDesc(TicketStatus.DONE_CANDIDATE);
    }
}
