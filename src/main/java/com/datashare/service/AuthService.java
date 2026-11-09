package com.datashare.service;

import com.datashare.entity.User;
import com.datashare.exception.BusinessException;
import com.datashare.model.AuthResponse;
import com.datashare.model.LoginRequest;
import com.datashare.model.RegisterRequest;
import com.datashare.repository.UserRepository;
import com.datashare.configuration.security.DataShareUserDetails;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private String dummyHash;

    @PostConstruct
    void initializeDummyHash() {
        this.dummyHash = passwords.encode("dummy-password-for-unknown-account");
    }


    public AuthResponse register(RegisterRequest request) {
        validate(request.getPassword(), 8);
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw emailConflict();
        }
        var user = new User(UUID.randomUUID(), email, passwords.encode(request.getPassword()),
                OffsetDateTime.now(ZoneOffset.UTC));
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // La contrainte SQL couvre aussi deux inscriptions simultanees.
            throw emailConflict();
        }
        return new AuthResponse().token(jwt.generateToken(new DataShareUserDetails(user)));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        validate(request.getPassword(), 1);
        var user = userRepository.findByEmail(normalizeEmail(request.getEmail()));
        boolean matches = passwords.matches(request.getPassword(), user.map(User::getPasswordUser).orElse(dummyHash));
        if (user.isEmpty() || !matches) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.");
        }
        return new AuthResponse().token(jwt.generateToken(new DataShareUserDetails(user.get())));
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private static BusinessException emailConflict() {
        return new BusinessException(HttpStatus.CONFLICT, "Cet email est deja utilise.");
    }

    public static void validate(String password, int minimum) {
        if (password == null || password.isBlank() || password.length() < minimum
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Le mot de passe doit contenir au moins " + minimum + " caracteres et au plus 72 octets UTF-8.");
        }
    }
}
