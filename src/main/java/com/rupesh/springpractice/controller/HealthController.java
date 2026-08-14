package com.rupesh.springpractice.controller;

import com.rupesh.springpractice.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private HealthService healthService;
    //constructor injection is preferred over field injection because it makes the class easier to test and promotes immutability.
    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }
    @GetMapping("/ping")//ping endpoint is used to check if the service is running and healthy.
    public String ping(){
        return healthService.status();
    }
}
