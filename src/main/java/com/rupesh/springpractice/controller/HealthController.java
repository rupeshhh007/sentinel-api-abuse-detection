package com.rupesh.springpractice.controller;

import com.rupesh.springpractice.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private HealthService healthService;
    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }
    @GetMapping("/ping")
    public String ping(){
        return healthService.status();
    }
}
