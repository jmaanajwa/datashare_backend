package com.datashare.configuration.security;

import com.datashare.configuration.security.CustomUserDetailService;
import com.datashare.configuration.security.JwtAuthenticationFilter;
import com.datashare.service.JwtService;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final JwtService jwt = mock(JwtService.class);
    private final CustomUserDetailService users = mock(CustomUserDetailService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwt, users, JsonMapper.builder().findAndAddModules().build());
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void allowsAnonymousRequestWithoutBearer() throws Exception {
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwt, users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/auth/register", "/auth/login"})
    void publicAuthenticationEndpointsIgnoreStaleBearerWithContextPath(String endpoint) throws Exception {
        request.setContextPath("/api");
        request.setRequestURI("/api" + endpoint);
        request.setMethod("POST");
        request.addHeader("Authorization", "Bearer expired-token");
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwt, users);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void authenticatesCurrentAccountUsingCaseInsensitiveBearerHeader() throws Exception {
        request.addHeader("Authorization", "bEaReR   token  ");
        when(jwt.extractEmail("token")).thenReturn("alice@example.com");
        var user = User.withUsername("alice@example.com").password("hash").authorities("USER").build();
        when(users.loadUserByUsername("alice@example.com")).thenReturn(user);
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(user);
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidTokenReturnsContractJsonAndDoesNotContinue() throws Exception {
        request.addHeader("Authorization", "Bearer invalid");
        when(jwt.extractEmail("invalid")).thenThrow(new JwtException("invalid signature"));
        filter.doFilter(request, response, chain);
        assertRejected();
        assertThat(response.getContentAsString()).contains("\"code\":401");
        verifyNoInteractions(users);
    }

    @Test
    void refusesTokenForRemovedAccount() throws Exception {
        request.addHeader("Authorization", "Bearer token");
        when(jwt.extractEmail("token")).thenReturn("missing@example.com");
        when(users.loadUserByUsername("missing@example.com")).thenThrow(new UsernameNotFoundException("missing"));
        filter.doFilter(request, response, chain);
        assertRejected();
    }

    @Test
    void refusesDisabledAccount() throws Exception {
        request.addHeader("Authorization", "Bearer token");
        when(jwt.extractEmail("token")).thenReturn("alice@example.com");
        when(users.loadUserByUsername("alice@example.com")).thenReturn(User.withUsername("alice@example.com")
                .password("hash").disabled(true).authorities("USER").build());
        filter.doFilter(request, response, chain);
        assertRejected();
    }

    private void assertRejected() {
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(chain);
    }
}
