package com.datashare.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/** Generation et verification des JWT selon le principe du projet Etudiant (JJWT / HS256). */
@Service
public class JwtService {
    // Cle de developpement stable, distincte de celle du projet 2. Surcharge possible par JWT_SECRET.
    private static final String LOCAL_SECRET_KEY = "ZDaTNbbKzaPDbR046/mzmIRNEkpDlVtsMriJUJ7fR0k=";

    private final SecretKey signingKey;
    private final JwtParser parser;
    private final String issuer;
    private final Duration ttl;

    public JwtService(@Value("${app.jwt.secret:}") String secret,
                      @Value("${app.jwt.issuer}") String issuer,
                      @Value("${app.jwt.ttl:PT24H}") Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("La duree de validite JWT doit etre positive.");
        }
        String encodedKey = secret == null || secret.isBlank() ? LOCAL_SECRET_KEY : secret;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(encodedKey));
        this.issuer = issuer;
        this.ttl = ttl;
        this.parser = Jwts.parser().verifyWith(signingKey).requireIssuer(issuer).build();
    }

    /** Le sujet est l'email utilise pour retrouver le compte a chaque requete. */
    public String generateToken(UserDetails userDetails) {
        Instant now = Instant.now();
        return Jwts.builder().issuer(issuer).subject(userDetails.getUsername())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(ttl)))
                .id(UUID.randomUUID().toString()).signWith(signingKey, Jwts.SIG.HS256).compact();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    /** Verifie la signature, l'emetteur et l'expiration avant d'exposer les claims. */
    public Claims extractClaims(String token) {
        var signed = parser.parseSignedClaims(token);
        if (!"HS256".equals(signed.getHeader().getAlgorithm())) {
            throw new JwtException("Algorithme JWT non autorise.");
        }
        Claims claims = signed.getPayload();
        if (claims.getExpiration() == null || claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new JwtException("Sujet ou expiration JWT absent.");
        }
        return claims;
    }
}
