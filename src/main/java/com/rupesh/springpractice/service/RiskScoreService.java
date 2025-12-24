package com.rupesh.springpractice.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RiskScoreService {

    public RiskResult evaluate(boolean rateAbuse, EntropyLevel entropy) {

        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (rateAbuse) {
            score += 40;
            reasons.add("High request rate detected");
        }

        if (entropy == EntropyLevel.LOW) {
            score += 50;
            reasons.add("Highly repetitive behavior detected");
        } else if (entropy == EntropyLevel.MEDIUM) {
            score += 20;
            reasons.add("Moderately predictable behavior detected");
        }

        return new RiskResult(score, reasons);
    }
}
