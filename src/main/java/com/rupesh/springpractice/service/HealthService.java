package com.rupesh.springpractice.service;

import org.springframework.stereotype.Service;

@Service
public class HealthService {
    public String status(){
        return "Sentinel core running";
    }
}
