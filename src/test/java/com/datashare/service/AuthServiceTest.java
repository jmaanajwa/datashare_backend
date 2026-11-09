package com.datashare.service;

import com.datashare.configuration.security.DataShareUserDetails;
import com.datashare.entity.User;
import com.datashare.exception.BusinessException;
import com.datashare.model.LoginRequest;
import com.datashare.model.RegisterRequest;
import com.datashare.repository.UserRepository;
import com.datashare.service.AuthService;
import com.datashare.service.JwtService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private AuthService service;

    @BeforeEach
    void setUp() {
        when(passwords.encode("dummy-password-for-unknown-account")).thenReturn("dummy-hash");
        when(passwords.encode("StrongPass123")).thenReturn("encoded-password");
        service = new AuthService(users, passwords, jwt);
        ReflectionTestUtils.invokeMethod(service, "initializeDummyHash");
    }

    @Test
    void registerNormalizesEmailHashesPasswordAndReturnsToken() {
        when(users.existsByEmail("alice@example.com")).thenReturn(false);
        when(jwt.generateToken(any(DataShareUserDetails.class))).thenReturn("jwt-token");

        var response = service.register(new RegisterRequest().email(" Alice@Example.com ").password("StrongPass123"));

        assertThat(response.getToken()).isEqualTo("jwt-token");
        var captor = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(captor.getValue().getPasswordUser()).isEqualTo("encoded-password");
    }

    @Test
    void registerRejectsExistingEmailAndRaceCondition() {
        when(users.existsByEmail("alice@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterRequest().email("alice@example.com").password("StrongPass123")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getStatus().value()).isEqualTo(409));

        when(users.existsByEmail("bob@example.com")).thenReturn(false);
        doThrow(new DataIntegrityViolationException("duplicate")).when(users).saveAndFlush(any(User.class));
        assertThatThrownBy(() -> service.register(new RegisterRequest().email("bob@example.com").password("StrongPass123")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getStatus().value()).isEqualTo(409));
    }

    @Test
    void loginUsesDummyHashForUnknownUserAndReturnsSameUnauthorizedError() {
        when(users.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        when(passwords.matches("wrong", "dummy-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest().email("missing@example.com").password("wrong")))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.getStatus().value()).isEqualTo(401);
                    assertThat(ex.getMessage()).isEqualTo("Email ou mot de passe incorrect.");
                });
        verify(passwords).matches("wrong", "dummy-hash");
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        var user = new User(UUID.randomUUID(), "alice@example.com", "encoded-password",
                OffsetDateTime.now(ZoneOffset.UTC));
        when(users.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwords.matches("StrongPass123", "encoded-password")).thenReturn(true);
        when(jwt.generateToken(any(DataShareUserDetails.class))).thenReturn("jwt-token");

        assertThat(service.login(new LoginRequest().email("ALICE@example.com").password("StrongPass123")).getToken())
                .isEqualTo("jwt-token");
    }

    @Test
    void validateRejectsBlankShortAndOversizedPasswords() {
        for (String password : new String[]{null, "", "short", "é".repeat(40)}) {
            assertThatThrownBy(() -> AuthService.validate(password, 8)).isInstanceOf(BusinessException.class);
        }
    }
}
