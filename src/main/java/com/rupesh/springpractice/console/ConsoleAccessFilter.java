package com.rupesh.springpractice.console;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component @Order(-10)
public class ConsoleAccessFilter extends OncePerRequestFilter {
    private final String token;
    public ConsoleAccessFilter(@Value("${sentinel.console.token:}") String token) {this.token=token;}
    @Override protected boolean shouldNotFilter(HttpServletRequest r) {return !r.getRequestURI().startsWith("/api/console/");}
    @Override protected void doFilterInternal(HttpServletRequest r,HttpServletResponse s,FilterChain chain) throws IOException,ServletException {
        String peer=r.getRemoteAddr();
        boolean local="127.0.0.1".equals(peer)||"::1".equals(peer)||"0:0:0:0:0:0:0:1".equals(peer);
        String host=r.getServerName();
        local=local && ("localhost".equalsIgnoreCase(host)||"127.0.0.1".equals(host)||"[::1]".equals(host)||"::1".equals(host));
        String auth=r.getHeader("Authorization");
        boolean authenticated=!token.isBlank() && auth!=null && MessageDigest.isEqual(
            ("Bearer "+token).getBytes(StandardCharsets.UTF_8),auth.getBytes(StandardCharsets.UTF_8));
        if((!token.isBlank()&&!authenticated)||(token.isBlank()&&!local)) {s.sendError(401,"Console authentication required");return;}
        if(!"GET".equals(r.getMethod()) && !"sentinel".equals(r.getHeader("X-Sentinel-Console"))) {s.sendError(403,"Console request header required");return;}
        s.setHeader("Cache-Control","no-store");
        s.setHeader("X-Content-Type-Options","nosniff");
        chain.doFilter(r,s);
    }
}
