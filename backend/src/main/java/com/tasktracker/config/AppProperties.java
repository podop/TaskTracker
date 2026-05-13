package com.tasktracker.config;

import com.tasktracker.model.MockUser;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors, List<MockUser> mockUsers) {

    public record Cors(List<String> allowedOrigins) {
    }
}
