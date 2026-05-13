package com.tasktracker.controller;

import com.tasktracker.dto.MockUserResponse;
import com.tasktracker.service.MockUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class MockUserController {

    private final MockUserService users;

    public MockUserController(MockUserService users) {
        this.users = users;
    }

    @GetMapping
    public List<MockUserResponse> list() {
        return users.findAll().stream().map(MockUserResponse::from).toList();
    }
}
