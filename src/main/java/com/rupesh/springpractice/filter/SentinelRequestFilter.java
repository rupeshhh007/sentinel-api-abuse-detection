package com.rupesh.springpractice.filter;

import com.rupesh.springpractice.service.*;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j // Lombok annotation for automatic logging
@Component
public class SentinelRequestFilter extends OncePerRequestFilter {

    private final FingerprintService fingerprintService;
    private final RateLimitService rateLimitService;
    private final EntropyService entropyService;
    private final RiskScoreService riskScoreService;

    public SentinelRequestFilter(
            FingerprintService fingerprintService,
            RateLimitService rateLimitService,
            EntropyService entropyService,
            RiskScoreService riskScoreService
    ) {
        this.fingerprintService = fingerprintService;
        this.rateLimitService = rateLimitService;
        this.entropyService = entropyService;
        this.riskScoreService = riskScoreService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String ip = normalizeIp(getClientIp(request));
        String userAgent = request.getHeader("User-Agent");
        String uri = request.getRequestURI();

        String fingerprint = fingerprintService.generate(ip, userAgent, uri);
        String shortFp = fingerprint.substring(0, 12);

        log.debug("Processing request | FP={} | IP={} | URI={}", shortFp, ip, uri);

        long now = System.currentTimeMillis() / 1000;

        // 1. Evaluate Entropy
        EntropyLevel entropy = entropyService.recordAndEvaluate(fingerprint, now, uri);
        if (entropy == EntropyLevel.LOW) {
            log.info("[SENTINEL][ENTROPY] Low entropy detected for FP={}", shortFp);
        }

        // 2. Evaluate Rate Limit
        boolean suspicious = rateLimitService.isSuspicious(fingerprint);
        if (suspicious) {
            log.warn("[SENTINEL][RATE] Suspicious activity detected for FP={}", shortFp);
        }

        // 3. Calculate Final Risk
        RiskResult risk = riskScoreService.evaluate(suspicious, entropy);

        // 4. BLOCK THE REQUEST IF RISK IS HIGH
        if (risk.getScore() >= 60) {
            log.warn("[SENTINEL][BLOCK] score={} FP={} reasons={}", risk.getScore(), shortFp, risk.getReasons());

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(String.format("{\"error\": \"Security constraint violation\", \"risk_score\": %d}", risk.getScore()));

            // Return immediately to stop the filter chain. The controller is never reached.
            return;
        }

        // If safe, continue to the controller
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only filter actual client requests, not internal dispatcher forwards
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