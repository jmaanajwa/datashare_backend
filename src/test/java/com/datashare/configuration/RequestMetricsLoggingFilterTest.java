package com.datashare.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestMetricsLoggingFilterTest {
    private final RequestMetricsLoggingFilter filter = new RequestMetricsLoggingFilter();

    @Test
    void logsSuccessfulShareRequestAndMasksToken() throws Exception {
        var request = request("POST", "/api/shares/share-token-that-must-not-be-logged/download");
        var response = new MockHttpServletResponse();
        response.setContentLength(1856);

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_OK));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
    }

    @Test
    void logsClientErrorAndMasksFileId() throws Exception {
        var request = request("GET", "/api/files/4f4d7a2b-5a8b-4d91-9a86-7d234fd3e2b1");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_NOT_FOUND));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
    }

    @Test
    void toleratesAnInvalidContentLengthOnServerError() throws Exception {
        var request = request("GET", "/api/health");
        var response = mock(HttpServletResponse.class);
        when(response.getHeader("Content-Length")).thenReturn("unknown");
        when(response.getStatus()).thenReturn(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR));

        org.mockito.Mockito.verify(response).setHeader(org.mockito.ArgumentMatchers.eq("X-Request-Id"),
                org.mockito.ArgumentMatchers.anyString());
    }

    private static MockHttpServletRequest request(String method, String uri) {
        var request = new MockHttpServletRequest(method, uri);
        request.setContextPath("/api");
        return request;
    }
}
