package com.rupesh.springpractice.filter;

import com.rupesh.springpractice.service.FingerprintService;
import com.rupesh.springpractice.detection.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Component
public class SentinelRequestFilter extends OncePerRequestFilter {
    private final FingerprintService fingerprints;
    private final DecisionService decisions;
    private final PolicyService policies;
    private final ObjectMapper json;
    public SentinelRequestFilter(FingerprintService fingerprints,DecisionService decisions,PolicyService policies,ObjectMapper json) {
        this.fingerprints=fingerprints;this.decisions=decisions;this.policies=policies;this.json=json;
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getRequestURI();
        return request.getDispatcherType()!=DispatcherType.REQUEST || !(path.startsWith("/api/protected/") || path.equals("/ping"));
    }
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws IOException,ServletException {
        // The socket peer is authoritative. Do not trust client-supplied X-Forwarded-For.
        String ip=request.getRemoteAddr();
        if("::1".equals(ip)||"0:0:0:0:0:0:0:1".equals(ip)) ip="127.0.0.1";
        String source;
        try { source=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(ip.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
        String route=request.getRequestURI().replaceAll("/[0-9]+(?=/|$)","/:id");
        if(route.length()>200) route=route.substring(0,200);
        var d=decisions.evaluate(fingerprints.generate(request,ip),source,route,request.getMethod());
        response.setHeader("X-Sentinel-Decision",d.id());
        response.setHeader("X-Sentinel-Action",d.action());
        response.setHeader("X-Sentinel-Score",String.valueOf(d.score()));
        if(d.action().equals("BLOCK")||d.action().equals("UNAVAILABLE")) {
            response.setStatus(d.action().equals("BLOCK")?429:503);
            response.setContentType("application/json");
            response.setHeader("Retry-After",String.valueOf(policies.get().windowSeconds()));
            json.writeValue(response.getWriter(),Map.of("error",d.action().equals("BLOCK")?"Security constraint violation":"Detection unavailable",
                "risk_score",d.score(),"decision_id",d.id(),"action",d.action()));
            return;
        }
        chain.doFilter(request,response);
    }
}
