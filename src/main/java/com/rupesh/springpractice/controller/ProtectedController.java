package com.rupesh.springpractice.controller;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/protected")
public class ProtectedController {
    @GetMapping("/{route}") public Map<String,Object> resource(@PathVariable String route) {
        return Map.of("resource",route,"message","Request passed Sentinel enforcement","timestamp",System.currentTimeMillis());
    }
}
