package com.tasktracker.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketStatusTest {

    @Test
    void todoCanOnlyTransitionToInProgress() {
        assertThat(TicketStatus.TODO.canUserTransitionTo(TicketStatus.IN_PROGRESS)).isTrue();
        assertThat(TicketStatus.TODO.canUserTransitionTo(TicketStatus.DONE_CANDIDATE)).isFalse();
        assertThat(TicketStatus.TODO.canUserTransitionTo(TicketStatus.DONE)).isFalse();
    }

    @Test
    void inProgressCanOnlyTransitionToDoneCandidate() {
        assertThat(TicketStatus.IN_PROGRESS.canUserTransitionTo(TicketStatus.DONE_CANDIDATE)).isTrue();
        assertThat(TicketStatus.IN_PROGRESS.canUserTransitionTo(TicketStatus.TODO)).isFalse();
        assertThat(TicketStatus.IN_PROGRESS.canUserTransitionTo(TicketStatus.DONE)).isFalse();
    }

    @Test
    void doneCandidateAndDoneAreUserTerminal() {
        assertThat(TicketStatus.DONE_CANDIDATE.canUserTransitionTo(TicketStatus.DONE)).isFalse();
        assertThat(TicketStatus.DONE.canUserTransitionTo(TicketStatus.TODO)).isFalse();
    }

    @Test
    void onlyDoneCandidateAllowsManagerMarkDone() {
        assertThat(TicketStatus.DONE_CANDIDATE.canManagerMarkDone()).isTrue();
        assertThat(TicketStatus.TODO.canManagerMarkDone()).isFalse();
        assertThat(TicketStatus.IN_PROGRESS.canManagerMarkDone()).isFalse();
        assertThat(TicketStatus.DONE.canManagerMarkDone()).isFalse();
    }
}
