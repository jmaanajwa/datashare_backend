package com.datashare.service;

import com.datashare.service.JwtService;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.userdetails.User;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {
    private static final String KEY = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
    private static final String ISSUER = "http://localhost:8080/api";
    private final JwtService jwt = new JwtService(KEY, ISSUER, Duration.ofDays(1));

    @Test
    void signsEmailForTwentyFourHoursWithoutPasswordClaims() {
        var principal = User.withUsername("alice@example.com").password("hash").authorities("USER").build();
        var claims = jwt.extractClaims(jwt.generateToken(principal));
        assertThat(claims.getSubject()).isEqualTo("alice@example.com");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(86400000L);
        assertThat(claims).doesNotContainKeys("password", "passwordHash");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid.token", "not-a-jwt"})
    void rejectsMissingOrMalformedToken(String token) {
        assertThatThrownBy(() -> jwt.extractEmail(token)).isInstanceOfAny(JwtException.class, IllegalArgumentException.class);
    }

    @Test
    void rejectsExpiredTokenAndWrongIssuer() {
        var key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(KEY));
        String expired = Jwts.builder().issuer(ISSUER).subject("alice@example.com")
                .expiration(Date.from(Instant.now().minusSeconds(120))).signWith(key, Jwts.SIG.HS256).compact();
        String wrongIssuer = Jwts.builder().issuer("other").subject("alice@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(60))).signWith(key, Jwts.SIG.HS256).compact();
        assertThatThrownBy(() -> jwt.extractEmail(expired)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwt.extractEmail(wrongIssuer)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsMissingExpiryAndMissingSubject() {
        var key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(KEY));
        String noExpiry = Jwts.builder().issuer(ISSUER).subject("alice@example.com").signWith(key, Jwts.SIG.HS256).compact();
        String noSubject = Jwts.builder().issuer(ISSUER).expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(key, Jwts.SIG.HS256).compact();
        assertThatThrownBy(() -> jwt.extractEmail(noExpiry)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwt.extractEmail(noSubject)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsAnotherSigningKey() {
        String token = Jwts.builder().issuer(ISSUER).subject("alice@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Jwts.SIG.HS256.key().build(), Jwts.SIG.HS256).compact();
        assertThatThrownBy(() -> jwt.extractEmail(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsOtherAlgorithmsEvenWithTheCorrectKey() {
        var key = Jwts.SIG.HS512.key().build();
        var service = new JwtService(java.util.Base64.getEncoder().encodeToString(key.getEncoded()), ISSUER, Duration.ofDays(1));
        String token = Jwts.builder().issuer(ISSUER).subject("alice@example.com")
                .expiration(Date.from(Instant.now().plusSeconds(60))).signWith(key, Jwts.SIG.HS512).compact();
        assertThatThrownBy(() -> service.extractEmail(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void localKeyWorksWithoutEnvironmentConfigurationAndIsStable() {
        var local = new JwtService("", ISSUER, Duration.ofDays(1));
        String token = local.generateToken(User.withUsername("alice@example.com").password("hash").authorities("USER").build());
        assertThat(new JwtService("", ISSUER, Duration.ofDays(1)).extractEmail(token)).isEqualTo("alice@example.com");
        assertThatThrownBy(() -> jwt.extractEmail(token)).isInstanceOf(JwtException.class);
    }
}
