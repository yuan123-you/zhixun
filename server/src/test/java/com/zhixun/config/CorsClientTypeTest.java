package com.zhixun.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import static org.junit.jupiter.api.Assertions.*;

class CorsClientTypeTest {
    @Test
    void approvedOriginCanUseTheClientIdentityHeaderSentByBothFrontends() throws Exception {
        var config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", "https://approved.example");
        var request = new MockHttpServletRequest("OPTIONS", "/v1/auth/login");
        request.addHeader("Origin", "https://approved.example");
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "content-type,x-client-type,x-xsrf-token");
        var response = new MockHttpServletResponse();
        var source = config.corsConfigurationSource();
        assertTrue(new DefaultCorsProcessor().processRequest(source.getCorsConfiguration(request), request, response));
        assertEquals(200, response.getStatus());
        assertTrue(response.getHeader("Access-Control-Allow-Headers").toLowerCase().contains("x-client-type"));
        assertEquals("https://approved.example", response.getHeader("Access-Control-Allow-Origin"));
    }
}
