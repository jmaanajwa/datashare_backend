package com.datashare.controller;

import com.datashare.exception.GlobalExceptionHandler;
import com.datashare.model.AuthResponse;
import com.datashare.model.LoginRequest;
import com.datashare.model.RegisterRequest;
import com.datashare.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiControllerTest {
    private final AuthService authService = mock(AuthService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new AuthApiController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginReturnsTokenAndSuccessStatus() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(new AuthResponse().token("jwt-token"));

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"StrongPass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void registerReturnsCreatedAndToken() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(new AuthResponse().token("new-token"));

        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.com\",\"password\":\"StrongPass123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("new-token"));
    }
}
