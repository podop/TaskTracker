package com.tasktracker.service;

import com.tasktracker.config.AppProperties;
import com.tasktracker.dto.CreateTicketRequest;
import com.tasktracker.exception.IllegalTransitionException;
import com.tasktracker.exception.NotFoundException;
import com.tasktracker.exception.UnknownUserException;
import com.tasktracker.model.MockUser;
import com.tasktracker.model.Ticket;
import com.tasktracker.model.TicketStatus;
import com.tasktracker.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TicketServiceTest {

    private TicketRepository tickets;
    private MockUserService userService;
    private TicketService service;

    @BeforeEach
    void setUp() {
        tickets = mock(TicketRepository.class);
        userService = new MockUserService(new AppProperties(
                new AppProperties.Cors(List.of()),
                List.of(new MockUser("u-001", "alice", "Alice"))
        ));
        service = new TicketService(tickets, userService);
        when(tickets.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            stampLifecycleFields(t);
            return t;
        });
    }

    @Test
    void createsTicketInTodoStatus() {
        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        service.create(new CreateTicketRequest("Fix login bug", "details"));
        verify(tickets).save(captor.capture());
        Ticket saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.TODO);
        assertThat(saved.getTitle()).isEqualTo("Fix login bug");
    }

    @Test
    void assignsToKnownMockUserByUsername() {
        Ticket t = persistedTicket(TicketStatus.TODO);
        Ticket assigned = service.assign(t.getId(), "alice");
        assertThat(assigned.getAssigneeId()).isEqualTo("u-001");
    }

    @Test
    void assignsByUserId() {
        Ticket t = persistedTicket(TicketStatus.TODO);
        Ticket assigned = service.assign(t.getId(), "u-001");
        assertThat(assigned.getAssigneeId()).isEqualTo("u-001");
    }

    @Test
    void rejectsAssignToUnknownUser() {
        Ticket t = persistedTicket(TicketStatus.TODO);
        assertThatThrownBy(() -> service.assign(t.getId(), "ghost"))
                .isInstanceOf(UnknownUserException.class);
    }

    @Test
    void assignReturnsNotFoundForMissingTicket() {
        UUID id = UUID.randomUUID();
        when(tickets.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.assign(id, "alice"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void allowsTodoToInProgress() {
        Ticket t = persistedTicket(TicketStatus.TODO);
        Ticket moved = service.userTransition(t.getId(), TicketStatus.IN_PROGRESS);
        assertThat(moved.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void allowsInProgressToDoneCandidate() {
        Ticket t = persistedTicket(TicketStatus.IN_PROGRESS);
        Ticket moved = service.userTransition(t.getId(), TicketStatus.DONE_CANDIDATE);
        assertThat(moved.getStatus()).isEqualTo(TicketStatus.DONE_CANDIDATE);
    }

    @Test
    void rejectsTodoDirectlyToDoneCandidate() {
        Ticket t = persistedTicket(TicketStatus.TODO);
        assertThatThrownBy(() -> service.userTransition(t.getId(), TicketStatus.DONE_CANDIDATE))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void rejectsUserMovingToDone() {
        Ticket t = persistedTicket(TicketStatus.DONE_CANDIDATE);
        assertThatThrownBy(() -> service.userTransition(t.getId(), TicketStatus.DONE))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void managerMarksDoneCandidateAsDone() {
        Ticket t = persistedTicket(TicketStatus.DONE_CANDIDATE);
        Ticket done = service.markDone(t.getId());
        assertThat(done.getStatus()).isEqualTo(TicketStatus.DONE);
    }

    @Test
    void managerCannotMarkTodoAsDone() {
        Ticket t = persistedTicket(TicketStatus.TODO);
        assertThatThrownBy(() -> service.markDone(t.getId()))
                .isInstanceOf(IllegalTransitionException.class);
    }

    @Test
    void getThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(tickets.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(id)).isInstanceOf(NotFoundException.class);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void listWithoutAssigneeFilterUsesExistingQueries(String blankAssignee) {
        List<Ticket> all = List.of(new Ticket("a", "d"));
        List<Ticket> byStatus = List.of(new Ticket("b", "d"));
        when(tickets.findAllByOrderByUpdatedAtDesc()).thenReturn(all);
        when(tickets.findAllByStatusOrderByUpdatedAtDesc(TicketStatus.TODO)).thenReturn(byStatus);

        assertThat(service.list(null, blankAssignee)).isSameAs(all);
        assertThat(service.list(TicketStatus.TODO, blankAssignee)).isSameAs(byStatus);
        verify(tickets, never()).findAllByAssigneeIdOrderByUpdatedAtDesc(any());
        verify(tickets, never()).findAllByStatusAndAssigneeIdOrderByUpdatedAtDesc(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"alice", "u-001", " ALICE "})
    void listFiltersByResolvedAssigneeId(String assignee) {
        List<Ticket> mine = List.of(new Ticket("a", "d"));
        when(tickets.findAllByAssigneeIdOrderByUpdatedAtDesc("u-001")).thenReturn(mine);

        assertThat(service.list(null, assignee)).isSameAs(mine);
        verify(tickets).findAllByAssigneeIdOrderByUpdatedAtDesc("u-001");
    }

    @Test
    void listCombinesStatusAndAssigneeFilters() {
        List<Ticket> mine = List.of(new Ticket("a", "d"));
        when(tickets.findAllByStatusAndAssigneeIdOrderByUpdatedAtDesc(TicketStatus.IN_PROGRESS, "u-001"))
                .thenReturn(mine);

        assertThat(service.list(TicketStatus.IN_PROGRESS, "alice")).isSameAs(mine);
        verify(tickets).findAllByStatusAndAssigneeIdOrderByUpdatedAtDesc(TicketStatus.IN_PROGRESS, "u-001");
    }

    @Test
    void listRejectsUnknownAssigneeBeforeReadingTickets() {
        assertThatThrownBy(() -> service.list(TicketStatus.TODO, "ghost"))
                .isInstanceOf(UnknownUserException.class)
                .hasMessage("Unknown mock user: ghost");
        verifyNoInteractions(tickets);
    }

    private Ticket persistedTicket(TicketStatus status) {
        Ticket t = new Ticket("title", "desc");
        setField(t, "status", status);
        stampLifecycleFields(t);
        when(tickets.findById(t.getId())).thenReturn(Optional.of(t));
        return t;
    }

    private static void stampLifecycleFields(Ticket t) {
        if (t.getCreatedAt() == null) {
            setField(t, "createdAt", Instant.now());
        }
        setField(t, "updatedAt", Instant.now());
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = Ticket.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
