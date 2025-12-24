package com.rupesh.springpractice.service;

import java.util.List;

public class RiskResult {

    private final int score;
    private final List<String> reasons;

    public RiskResult(int score, List<String> reasons) {
        this.score = score;
        this.reasons = reasons;
    }

    public int getScore() {
        return score;
    }

    public List<String> getReasons() {
        return reasons;
    }
}
