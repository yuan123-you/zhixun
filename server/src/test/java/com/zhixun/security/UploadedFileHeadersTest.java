package com.zhixun.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;

class UploadedFileHeadersTest {
    @ParameterizedTest
    @ValueSource(strings = {"/uploads/images/upload.svg", "/uploads/files/upload.html", "/v1/files/view/zhixun/images/upload.svg"})
    void uploadedDocumentsCannotExecuteInTheApplicationOrigin(String path) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api" + path);
        request.setContextPath("/api"); request.setServletPath(path);
        var response = new MockHttpServletResponse();
        String body = "<svg xmlns='http://www.w3.org/2000/svg' onload='alert(1)'/>";
        new SecurityHeadersFilter().doFilter(request, response, (req, res) -> res.getWriter().write(body));
        String policy = response.getHeader("Content-Security-Policy");
        assertNotNull(policy);
        assertTrue(policy.contains("sandbox"), "Uploaded documents require an opaque sandboxed origin");
        assertFalse(policy.contains("allow-scripts"));
        assertFalse(policy.contains("allow-same-origin"));
        assertTrue(policy.contains("default-src 'none'"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals(body, response.getContentAsString());
    }

    @Test
    void normalApiResponsesKeepTheirExistingPolicy() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/articles");
        request.setServletPath("/v1/articles");
        var response = new MockHttpServletResponse();
        new SecurityHeadersFilter().doFilter(request, response, (req, res) -> {});
        assertFalse(response.getHeader("Content-Security-Policy").contains("sandbox"));
    }
}
