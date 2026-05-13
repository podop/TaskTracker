package com.tasktracker.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties properties;

    public WebConfig(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        var origins = properties.cors() != null && properties.cors().allowedOrigins() != null
                ? properties.cors().allowedOrigins().toArray(String[]::new)
                : new String[]{"http://localhost:5173"};
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}
