package com.rupesh.springpractice.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class FingerprintService {

    public String generate(HttpServletRequest request, String normalizedIp) {
        // Extract standard headers
        String userAgent = getOrDefault(request.getHeader("User-Agent"));
        String acceptLang = getOrDefault(request.getHeader("Accept-Language"));
        String acceptEnc = getOrDefault(request.getHeader("Accept-Encoding"));

        // Extract modern browser client hints (bots rarely send these)
        String secChUa = getOrDefault(request.getHeader("Sec-CH-UA"));
        String secChUaPlatform = getOrDefault(request.getHeader("Sec-CH-UA-Platform"));

        // Notice we do NOT include the URI here.
        // We are fingerprinting the client's machine, not their action.
        String raw = normalizedIp + "|"
                + userAgent + "|"
                + acceptLang + "|"
                + acceptEnc + "|"
                + secChUa + "|"
                + secChUaPlatform;

        return sha256(raw);
    }

    // Helper method to ensure null headers don't break our string concatenation
    private String getOrDefault(String value) {
        return (value != null && !value.isEmpty()) ? value : "NONE";
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}