package com.datashare.controller;

import com.datashare.model.AuthResponse;
import com.datashare.model.LoginRequest;
import com.datashare.model.RegisterRequest;
import com.datashare.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthApiController implements AuthApi {

    private final AuthService authService;


    @Override
    public ResponseEntity<AuthResponse> loginUser(LoginRequest body) {
        return ResponseEntity.ok(authService.login(body));
    }

    @Override
    public ResponseEntity<AuthResponse> registerUser(RegisterRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(body));
    }
}
