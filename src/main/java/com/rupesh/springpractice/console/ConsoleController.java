package com.rupesh.springpractice.console;

import com.rupesh.springpractice.detection.PolicyService;
import com.rupesh.springpractice.detection.WindowStore;
import com.rupesh.springpractice.detection.DetectionModel.Policy;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.core.env.Environment;
import java.util.*;

@RestController @RequestMapping("/api/console")
public class ConsoleController {
    private final TelemetryService telemetry;
    private final PolicyService policies;
    private final ReplayService replay;
    private final Environment env;
    private final WindowStore store;
    public ConsoleController(TelemetryService telemetry,PolicyService policies,ReplayService replay,Environment env,WindowStore store) {
        this.telemetry=telemetry;this.policies=policies;this.replay=replay;this.env=env;this.store=store;
    }
    @GetMapping("/overview") public Map<String,Object> overview(@RequestParam(defaultValue="LIVE") String origin) {
        if(!List.of("LIVE","REPLAY").contains(origin))throw new IllegalArgumentException("Invalid origin");
        return Map.of("telemetry",telemetry.snapshot(origin),"policy",policies.get(),
            "storage",Arrays.asList(env.getActiveProfiles()).contains("demo")?"LOCAL_DEMO":"REDIS",
            "engineHealthy",store.healthy(),"scope","Instance-local telemetry; retained 1,000 decisions per origin");
    }
    @GetMapping("/decisions/{id}") public ResponseEntity<?> decision(@PathVariable String id) {
        return telemetry.find(id).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }
    @GetMapping("/policy") public Policy policy(){return policies.get();}
    @PutMapping("/policy") public Policy policy(@RequestBody Policy next){return policies.update(next);}
    @GetMapping("/scenarios") public List<ReplayService.Scenario> scenarios(){return replay.scenarios();}
    public record RunRequest(String scenario) {}
    @PostMapping("/replay") public ReplayService.ReplayResult replay(@RequestBody RunRequest r){return replay.run(r.scenario(),true);}
    @PostMapping("/evaluation") public Map<String,Object> evaluation(){return replay.evaluate();}
    @DeleteMapping("/replay") public Map<String,String> clear(){telemetry.clearReplay();return Map.of("status","cleared");}
    @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> invalid(IllegalArgumentException e){return Map.of("error",e.getMessage());}
    @ExceptionHandler(IllegalStateException.class) @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String,String> conflict(IllegalStateException e){return Map.of("error",e.getMessage());}
}
