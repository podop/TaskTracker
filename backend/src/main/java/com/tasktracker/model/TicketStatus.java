package com.tasktracker.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum TicketStatus {
    TODO,
    IN_PROGRESS,
    DONE_CANDIDATE,
    DONE;

    private static final Map<TicketStatus, Set<TicketStatus>> USER_TRANSITIONS = Map.of(
            TODO, EnumSet.of(IN_PROGRESS),
            IN_PROGRESS, EnumSet.of(DONE_CANDIDATE),
            DONE_CANDIDATE, EnumSet.noneOf(TicketStatus.class),
            DONE, EnumSet.noneOf(TicketStatus.class)
    );

    public boolean canUserTransitionTo(TicketStatus next) {
        return USER_TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }

    public boolean canManagerMarkDone() {
        return this == DONE_CANDIDATE;
    }
}
