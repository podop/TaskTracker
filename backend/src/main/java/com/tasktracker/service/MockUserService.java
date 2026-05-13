package com.tasktracker.service;

import com.tasktracker.config.AppProperties;
import com.tasktracker.model.MockUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MockUserService {

    private final List<MockUser> users;

    public MockUserService(AppProperties properties) {
        this.users = properties.mockUsers() == null ? List.of() : List.copyOf(properties.mockUsers());
    }

    public List<MockUser> findAll() {
        return users;
    }

    public Optional<MockUser> findByIdOrUsername(String idOrUsername) {
        if (idOrUsername == null) {
            return Optional.empty();
        }
        String trimmed = idOrUsername.trim();
        return users.stream()
                .filter(u -> u.id().equalsIgnoreCase(trimmed) || u.username().equalsIgnoreCase(trimmed))
                .findFirst();
    }

    public boolean exists(String idOrUsername) {
        return findByIdOrUsername(idOrUsername).isPresent();
    }
}
