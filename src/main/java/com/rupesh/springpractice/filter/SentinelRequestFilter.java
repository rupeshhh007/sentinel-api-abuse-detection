package com.rupesh.springpractice.filter;

import com.rupesh.springpractice.service.*;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component

public class SentinelRequestFilter extends OncePerRequestFilter {
    private String normalizeIp(String ip) {
        if (ip == null) return "unknown";

        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
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
        logSignals(request);
        String ip = normalizeIp(getClientIp(request));
        String userAgent = request.getHeader("User-Agent");
        String uri = request.getRequestURI();

        String fingerprint = fingerprintService.generate(ip, userAgent, uri);

        System.out.println(
                "FP=" + fingerprint.substring(0, 12) +
                        " | IP=" + ip +
                        " | URI=" + uri
        );
        long now = System.currentTimeMillis() / 1000;

        EntropyLevel entropy =
                entropyService.recordAndEvaluate(fingerprint, now, uri);

        if (entropy == EntropyLevel.LOW) {
            System.out.println(
                    "[SENTINEL][ENTROPY] Low entropy detected for FP="
                            + fingerprint.substring(0, 12)
            );
        }


        boolean suspicious = rateLimitService.isSuspicious(fingerprint);

        if (suspicious) {
            System.out.println(
                    "[SENTINEL][RATE] Suspicious activity detected for FP="
                            + fingerprint.substring(0, 12)
            );
        }

        RiskResult risk =
                riskScoreService.evaluate(suspicious, entropy);

        if (risk.getScore() >= 60) {
            System.out.println(
                    "[SENTINEL][RISK] score=" + risk.getScore()
                            + " FP=" + fingerprint.substring(0, 12)
                            + " reasons=" + risk.getReasons()
            );
        }
        System.out.println(
                "URI=" + request.getRequestURI() +
                        " | Method=" + request.getMethod() +
                        " | Dispatcher=" + request.getDispatcherType()
        );
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getDispatcherType() != DispatcherType.REQUEST;
    }
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0];
        }
        return request.getRemoteAddr();
    }
    private void logSignals(HttpServletRequest request) {
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String uri = request.getRequestURI();

        System.out.println(
                "IP=" + ip +
                        " | UA=" + userAgent +
                        " | URI=" + uri
        );
    }

}
