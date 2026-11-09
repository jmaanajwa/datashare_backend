package com.datashare.controller;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ControllerSupportTest {
    @Test
    void corsFilterAddsHeadersAndDelegatesRequest() throws Exception {
        var filter = new ApiOriginFilter();
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.init(new MockFilterConfig(new MockServletContext(), "cors"));
        filter.doFilter(mock(ServletRequest.class), response, chain);
        filter.destroy();

        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("*");
        assertThat(response.getHeader("Access-Control-Allow-Methods")).contains("GET", "POST", "DELETE", "PUT");
        assertThat(response.getHeader("Access-Control-Allow-Headers")).isEqualTo("Content-Type");
        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.same(response));
    }

    @Test
    void responseMessageMapsAllKnownAndUnknownCodes() {
        assertThat(new ApiResponseMessage(ApiResponseMessage.ERROR, "error").getType()).isEqualTo("error");
        assertThat(new ApiResponseMessage(ApiResponseMessage.WARNING, "warning").getType()).isEqualTo("warning");
        assertThat(new ApiResponseMessage(ApiResponseMessage.INFO, "info").getType()).isEqualTo("info");
        assertThat(new ApiResponseMessage(ApiResponseMessage.OK, "ok").getType()).isEqualTo("ok");
        assertThat(new ApiResponseMessage(ApiResponseMessage.TOO_BUSY, "busy").getType()).isEqualTo("too busy");
        var unknown = new ApiResponseMessage(99, "unknown");
        assertThat(unknown.getType()).isEqualTo("unknown");
        unknown.setCode(100);
        unknown.setType("custom");
        unknown.setMessage("changed");
        assertThat(unknown.getCode()).isEqualTo(100);
        assertThat(unknown.getType()).isEqualTo("custom");
        assertThat(unknown.getMessage()).isEqualTo("changed");
    }

    @Test
    void apiExceptionsKeepMessageAndType() {
        var apiException = new ApiException(400, "bad request");
        var notFound = new NotFoundException(404, "missing");
        assertThat(apiException).hasMessage("bad request");
        assertThat(notFound).isInstanceOf(ApiException.class).hasMessage("missing");
    }
}
