package com.datashare.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Emits one structured event for every HTTP request without logging secrets,
 * query strings or file contents.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RequestMetricsLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestMetricsLoggingFilter.class);
    private static final String REQUEST_ID = "request.id";

    @Override
    @SuppressWarnings("null")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID, requestId);
        response.setHeader("X-Request-Id", requestId);
        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            int status = response.getStatus();
            var event = status >= 500 ? log.atError() : status >= 400 ? log.atWarn() : log.atInfo();
            event.addKeyValue("event.name", "http_request")
                    .addKeyValue("http.request.method", request.getMethod())
                    .addKeyValue("url.path", safeRoute(request))
                    .addKeyValue("http.response.status_code", status)
                    .addKeyValue("http.response.body.bytes", responseContentLength(response))
                    .addKeyValue("http.server.duration_ms", durationMs)
                    .addKeyValue("outcome", status >= 400 ? "failure" : "success")
                    .log("HTTP request completed");
            MDC.remove(REQUEST_ID);
        }
    }

    private static String safeRoute(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String path = request.getRequestURI();
        String apiPath = path.startsWith(contextPath) ? path.substring(contextPath.length()) : path;
        String[] segments = apiPath.split("/", -1);
        for (int index = 1; index < segments.length - 1; index++) {
            if ("shares".equals(segments[index])) {
                segments[index + 1] = "{token}";
            } else if ("files".equals(segments[index])) {
                segments[index + 1] = "{fileId}";
            }
        }
        return contextPath + String.join("/", segments);
    }

    private static long responseContentLength(HttpServletResponse response) {
        String contentLength = response.getHeader("Content-Length");
        if (contentLength == null) {
            return -1;
        }
        try {
            return Long.parseLong(contentLength);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
