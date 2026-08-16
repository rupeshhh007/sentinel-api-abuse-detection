package com.rupesh.springpractice.filter;

import com.rupesh.springpractice.service.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class SentinelRequestFilter extends OncePerRequestFilter {

    private final FingerprintService fingerprintService;
    private final RateLimitService rateLimitService;
    private final EntropyService entropyService;
    private final RiskScoreService riskScoreService;

    @Value("${sentinel.risk.threshold:60}")
    private int riskThreshold;

    private final Counter requestsEvaluated;
    private final Counter requestsBlocked;
    private final Counter redisFailures;

    public SentinelRequestFilter(
            FingerprintService fingerprintService,
            RateLimitService rateLimitService,
            EntropyService entropyService,
            RiskScoreService riskScoreService,
            MeterRegistry meterRegistry
    ) {
        this.fingerprintService = fingerprintService;
        this.rateLimitService = rateLimitService;
        this.entropyService = entropyService;
        this.riskScoreService = riskScoreService;

        this.requestsEvaluated = meterRegistry.counter("sentinel.requests.evaluated");
        this.requestsBlocked = meterRegistry.counter("sentinel.requests.blocked");
        this.redisFailures = meterRegistry.counter("sentinel.redis.failures");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String ip = normalizeIp(getClientIp(request));
        String fingerprint = fingerprintService.generate(request, ip);
        String shortFp = fingerprint.substring(0, 12);
        long now = System.currentTimeMillis() / 1000;

        requestsEvaluated.increment();

        try {
            // 1. Evaluate Entropy
            EntropyLevel entropy = entropyService.recordAndEvaluate(fingerprint, now, request.getRequestURI());

            // 2. Evaluate Rate Limit
            boolean suspicious = rateLimitService.isSuspicious(fingerprint);

            // 3. Calculate Risk
            RiskResult risk = riskScoreService.evaluate(suspicious, entropy);

            // 4. Block if over threshold
            if (risk.getScore() >= riskThreshold) {
                log.warn("[SENTINEL][BLOCK] score={} FP={} reasons={}", risk.getScore(), shortFp, risk.getReasons());
                requestsBlocked.increment();

                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(String.format("{\"error\": \"Security constraint violation\", \"risk_score\": %d}", risk.getScore()));
                return;
            }
        } catch (Exception e) {
            // FAIL-OPEN: Log error, increment metric, and allow request through
            log.error("[SENTINEL][ERROR] Redis/Service failure evaluating FP={}. Failing open to preserve availability.", shortFp, e);
            redisFailures.increment();
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getDispatcherType() != DispatcherType.REQUEST;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalizeIp(String ip) {
        if (ip == null) return "unknown";
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}