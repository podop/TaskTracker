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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
    void containerIsRunning() {
        assertThat(postgres.isRunning()).isTrue();
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
