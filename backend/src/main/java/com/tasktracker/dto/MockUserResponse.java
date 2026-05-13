package com.tasktracker.dto;

import com.tasktracker.model.MockUser;

public record MockUserResponse(String id, String username, String displayName) {
    public static MockUserResponse from(MockUser u) {
        return new MockUserResponse(u.id(), u.username(), u.displayName());
    }
}
