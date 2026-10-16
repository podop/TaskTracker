package com.tasktracker.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tasktracker.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("it")
class TicketApiIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired TicketRepository tickets;
    @Autowired ObjectMapper json;

    @BeforeEach
    void cleanDb() {
        tickets.deleteAll();
    }

    @Test
    void createsTicket() throws Exception {
        mvc.perform(post("/api/tickets")
                        .contentType("application/json")
                        .content("""
                                {"title": "Fix login", "description": "details"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.title").value("Fix login"));
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        mvc.perform(post("/api/tickets")
                        .contentType("application/json")
                        .content("""
                                {"title": "", "description": "nope"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsTicketsOrderedByUpdatedAtDesc() throws Exception {
        String first = createTicket("first");
        String second = createTicket("second");

        mvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[1].id").value(first));
    }

    @Test
    void assignsToMockUser() throws Exception {
        String id = createTicket("assignable");
        mvc.perform(patch("/api/tickets/" + id + "/assignee")
                        .contentType("application/json")
                        .content("""
                                {"assignee": "alice"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").value("u-001"));
    }

    @Test
    void rejectsUnknownAssignee() throws Exception {
        String id = createTicket("assignable");
        mvc.perform(patch("/api/tickets/" + id + "/assignee")
                        .contentType("application/json")
                        .content("""
                                {"assignee": "ghost"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAssignOnMissingTicket() throws Exception {
        mvc.perform(patch("/api/tickets/00000000-0000-0000-0000-000000000000/assignee")
                        .contentType("application/json")
                        .content("""
                                {"assignee": "alice"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void runsFullTransitionWorkflow() throws Exception {
        String id = createTicket("workflow");
        changeStatus(id, "IN_PROGRESS").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        changeStatus(id, "DONE_CANDIDATE").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE_CANDIDATE"));
    }

    @Test
    void rejectsIllegalUserTransitionToDone() throws Exception {
        String id = createTicket("nope-done");
        changeStatus(id, "DONE").andExpect(status().isConflict());
    }

    @Test
    void rejectsSkippingInProgress() throws Exception {
        String id = createTicket("skipper");
        changeStatus(id, "DONE_CANDIDATE").andExpect(status().isConflict());
    }

    @Test
    void managerLifecycleHappyPath() throws Exception {
        String id = createTicket("approve-me");
        changeStatus(id, "IN_PROGRESS");
        changeStatus(id, "DONE_CANDIDATE");

        mvc.perform(get("/api/manager/tickets/done-candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mvc.perform(patch("/api/manager/tickets/" + id + "/mark-done"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        mvc.perform(get("/api/manager/tickets/done-candidates"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void managerCannotMarkTodoAsDone() throws Exception {
        String id = createTicket("not-ready");
        mvc.perform(patch("/api/manager/tickets/" + id + "/mark-done"))
                .andExpect(status().isConflict());
    }

    @Test
    void managerReturns404ForMissingTicket() throws Exception {
        mvc.perform(patch("/api/manager/tickets/00000000-0000-0000-0000-000000000000/mark-done"))
                .andExpect(status().isNotFound());
    }

    @Test
    void filtersListByStatus() throws Exception {
        String a = createTicket("a");
        String b = createTicket("b");
        changeStatus(a, "IN_PROGRESS");

        mvc.perform(get("/api/tickets").param("status", "TODO"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(b));
    }

    @Test
    void filtersListByAssigneeUsername() throws Exception {
        Seed s = seedAssignedTickets();

        mvc.perform(get("/api/tickets").param("assignee", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(s.t1, s.t2)));
    }

    @Test
    void filtersListByAssigneeId() throws Exception {
        Seed s = seedAssignedTickets();

        mvc.perform(get("/api/tickets").param("assignee", "u-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(s.t1, s.t2)));
    }

    @Test
    void filtersListByAssigneeIgnoringCaseAndSurroundingSpaces() throws Exception {
        Seed s = seedAssignedTickets();

        mvc.perform(get("/api/tickets").param("assignee", " ALICE "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(s.t1, s.t2)));
    }

    @Test
    void combinesAssigneeAndStatusFilters() throws Exception {
        Seed s = seedAssignedTickets();
        changeStatus(s.t1, "IN_PROGRESS");

        mvc.perform(get("/api/tickets").param("assignee", "alice").param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(s.t1));
    }

    @Test
    void returnsEmptyListWhenAssigneeHasNoTicketsInStatus() throws Exception {
        seedAssignedTickets();

        mvc.perform(get("/api/tickets").param("assignee", "bob").param("status", "DONE_CANDIDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsUnknownAssigneeFilterWithProblemDetails() throws Exception {
        seedAssignedTickets();

        mvc.perform(get("/api/tickets").param("assignee", "ghost"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Unknown mock user: ghost"))
                .andExpect(result -> result.getResponse().getHeaderNames().forEach(name ->
                        assertThat(result.getResponse().getHeaders(name)).noneMatch(v -> v.contains("ghost"))));
    }

    @Test
    void treatsBlankAssigneeAsNoFilter() throws Exception {
        Seed s = seedAssignedTickets();
        String unfiltered = mvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(s.t1, s.t2, s.t3, s.t4)))
                .andReturn().getResponse().getContentAsString();

        for (String blank : new String[] {"", "  "}) {
            String body = mvc.perform(get("/api/tickets").param("assignee", blank))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(body).isEqualTo(unfiltered);
        }
    }

    @Test
    void ordersAssigneeFilterByUpdatedAtDesc() throws Exception {
        Seed s = seedAssignedTickets();
        changeStatus(s.t1, "IN_PROGRESS");

        mvc.perform(get("/api/tickets").param("assignee", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(s.t1))
                .andExpect(jsonPath("$[1].id").value(s.t2));
    }

    @Test
    void returnsEmptyListForKnownAssigneeWithoutTickets() throws Exception {
        Seed s = seedAssignedTickets();
        assign(s.t3, "alice");

        mvc.perform(get("/api/tickets").param("assignee", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void movesTicketBetweenAssigneeFiltersOnReassignment() throws Exception {
        Seed s = seedAssignedTickets();
        assign(s.t3, "alice");

        mvc.perform(get("/api/tickets").param("assignee", "alice"))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(s.t1, s.t2, s.t3)));
        mvc.perform(get("/api/tickets").param("assignee", "bob"))
                .andExpect(jsonPath("$[*].id", not(hasItem(s.t3))));
    }

    @Test
    void reportsInvalidStatusBeforeUnknownAssignee() throws Exception {
        seedAssignedTickets();

        mvc.perform(get("/api/tickets").param("status", "NOT_A_STATUS").param("assignee", "ghost"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("Unknown mock user"));
    }

    @Test
    void rejectsRepeatedAssigneeParameter() throws Exception {
        seedAssignedTickets();

        mvc.perform(get("/api/tickets").param("assignee", "alice", "bob"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown mock user: alice,bob"));
    }

    @Test
    void keepsStatusFilterWorkingWithoutAssignee() throws Exception {
        Seed s = seedAssignedTickets();
        changeStatus(s.t1, "IN_PROGRESS");

        mvc.perform(get("/api/tickets").param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(s.t2, s.t3, s.t4)));
    }

    @Test
    void documentsAssigneeParameterInOpenApi() throws Exception {
        String body = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode op = json.readTree(body).at("/paths/~1api~1tickets/get");

        JsonNode assignee = null;
        for (JsonNode p : op.get("parameters")) {
            if ("assignee".equals(p.get("name").asText())) {
                assignee = p;
            }
        }
        assertThat(assignee).isNotNull();
        assertThat(assignee.get("in").asText()).isEqualTo("query");
        assertThat(assignee.path("required").asBoolean(false)).isFalse();
        assertThat(assignee.at("/schema/type").asText()).isEqualTo("string");
        assertThat(op.get("responses").has("200")).isTrue();
        assertThat(op.get("responses").has("400")).isTrue();
    }

    @Test
    void containerIsRunning() {
        assertThat(postgres.isRunning()).isTrue();
    }

    private record Seed(String t1, String t2, String t3, String t4) {}

    /** T1, T2 -> alice (T1 mutated last), T3 -> bob, T4 unassigned. */
    private Seed seedAssignedTickets() throws Exception {
        String t1 = createTicket("t1");
        String t2 = createTicket("t2");
        String t3 = createTicket("t3");
        String t4 = createTicket("t4");
        assign(t2, "alice");
        assign(t3, "bob");
        assign(t1, "alice");
        return new Seed(t1, t2, t3, t4);
    }

    private void assign(String id, String user) throws Exception {
        mvc.perform(patch("/api/tickets/" + id + "/assignee")
                        .contentType("application/json")
                        .content("{\"assignee\":\"" + user + "\"}"))
                .andExpect(status().isOk());
    }

    private String createTicket(String title) throws Exception {
        String body = mvc.perform(post("/api/tickets")
                        .contentType("application/json")
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return node.get("id").asText();
    }

    private org.springframework.test.web.servlet.ResultActions changeStatus(String id, String status) throws Exception {
        return mvc.perform(patch("/api/tickets/" + id + "/status")
                .contentType("application/json")
                .content("{\"status\":\"" + status + "\"}"));
    }
}
