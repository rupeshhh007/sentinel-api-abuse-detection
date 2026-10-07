package com.rupesh.springpractice.detection;
import com.rupesh.springpractice.console.ConsoleAccessFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;
class ConsoleAccessTest {
    @Test void configuredTokenIsRequiredEvenForLocalClients() throws Exception {
        var filter=new ConsoleAccessFilter("test-operator-token");
        var request=new MockHttpServletRequest("GET","/api/console/overview");
        var response=new MockHttpServletResponse();
        filter.doFilter(request,response,new MockFilterChain());
        assertEquals(401,response.getStatus());
        request.addHeader("Authorization","Bearer test-operator-token");
        request.setRemoteAddr("203.0.113.5");response=new MockHttpServletResponse();
        filter.doFilter(request,response,new MockFilterChain());assertEquals(200,response.getStatus());
    }
    @Test void localConsoleRejectsUntrustedHostNames() throws Exception {
        var filter=new ConsoleAccessFilter("");
        var request=new MockHttpServletRequest("GET","/api/console/overview");
        request.setServerName("attacker.example");
        var response=new MockHttpServletResponse();
        filter.doFilter(request,response,new MockFilterChain());
        assertEquals(401,response.getStatus());
    }
}
