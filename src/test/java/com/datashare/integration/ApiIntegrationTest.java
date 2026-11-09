package com.datashare.integration;

import com.datashare.PostgresTestConfiguration;
import com.datashare.repository.FileRepository;
import com.datashare.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.datashare.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Parcours HTTP avec filtres de securite, JWT reels, migrations SQL, JPA et disque temporaire. */
@SpringBootTest(properties = {"debug=false", "logging.level.org.springframework.web=INFO"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class ApiIntegrationTest {
    private static final String ISSUER = "http://localhost:8080/api";
    @TempDir
    static Path uploadDirectory;

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.directory", () -> uploadDirectory.toString());
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired FileRepository files;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtService jwt;
    @Value("${app.jwt.secret}") String signingSecret;

    @BeforeEach
    void clean() throws Exception {
        files.deleteAll();
        users.deleteAll();
        try (var paths = Files.list(uploadDirectory)) {
            for (Path path : paths.toList()) { Files.delete(path); }
        }
    }

    @Test
    void registrationPersistsHashAndReturnsValidExpiringJwt() throws Exception {
        String token = register("Alice@Example.com");
        var user = users.findByEmail("alice@example.com").orElseThrow();
        assertThat(user.getPasswordUser()).isNotEqualTo("StrongPass123");
        assertThat(passwords.matches("StrongPass123", user.getPasswordUser())).isTrue();
        Claims decoded = jwt.extractClaims(token);
        assertThat(decoded.getSubject()).isEqualTo(user.getEmail());
        assertThat(decoded.getIssuer()).isEqualTo(ISSUER);
        assertThat(decoded.getExpiration().toInstant()).isAfter(Instant.now());
        assertThat(decoded.getExpiration().getTime() - decoded.getIssuedAt().getTime()).isEqualTo(86400000L);
        assertThat(decoded).doesNotContainKeys("password", "passwordHash");
    }

    @Test
    void duplicateEmailIsCaseInsensitive() throws Exception {
        register("alice@example.com");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("ALICE@example.com", "StrongPass123")))
                .andExpect(status().isConflict()).andExpect(jsonPath("code").value(409));
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void registrationIgnoresStaleBearerAndReturnsNewJwt() throws Exception {
        var response = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer stale-or-invalid-token")
                        .content(json("alice@example.com", "StrongPass123")))
                .andExpect(status().isCreated()).andReturn().getResponse();
        String token = mapper.readTree(response.getContentAsString()).get("token").asText();
        assertThat(jwt.extractEmail(token)).isEqualTo("alice@example.com");
        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void loginIgnoresStaleBearerAndChecksSubmittedCredentials() throws Exception {
        register("alice@example.com");
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer stale-or-invalid-token")
                        .content(json("alice@example.com", "StrongPass123")))
                .andExpect(status().isOk()).andExpect(jsonPath("token").isNotEmpty());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer stale-or-invalid-token")
                        .content(json("alice@example.com", "WrongPassword")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("message").value("Email ou mot de passe incorrect."));
    }

    @Test
    void wrongMethodOnPublicAuthenticationEndpointsReturnsMethodNotAllowed() throws Exception {
        for (String endpoint : List.of("/auth/register", "/auth/login")) {
            mvc.perform(get(endpoint).header("Authorization", "Bearer stale-or-invalid-token"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Test
    void invalidEmailWeakBlankAndOversizedPasswordsAreRejected() throws Exception {
        for (var entry : List.of(
                Map.entry("invalid", "StrongPass123"), Map.entry("alice@example.com", "short"),
                Map.entry("alice@example.com", "        "), Map.entry("alice@example.com", "é".repeat(40)))) {
            mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                            .content(json(entry.getKey(), entry.getValue())))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value(400));
        }
        assertThat(users.count()).isZero();
    }

    @Test
    void loginReturnsJwtAndInvalidCredentialsGiveSameError() throws Exception {
        register("alice@example.com");
        var result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("ALICE@example.com", "StrongPass123")))
                .andExpect(status().isOk()).andReturn();
        assertThat(jwt.extractEmail(mapper.readTree(result.getResponse().getContentAsString()).get("token").asText()))
                .isEqualTo("alice@example.com");
        String previousMessage = null;
        for (String email : List.of("alice@example.com", "missing@example.com")) {
            var response = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json(email, "WrongPassword")))
                    .andExpect(status().isUnauthorized()).andReturn().getResponse();
            String message = mapper.readTree(response.getContentAsString()).get("message").asText();
            if (previousMessage != null) { assertThat(message).isEqualTo(previousMessage); }
            previousMessage = message;
        }
    }

    @Test
    void anonymousUploadStoresBytesAndDefaultExpiration() throws Exception {
        var response = mvc.perform(multipart("/files").file(document()))
                .andExpect(status().isCreated()).andExpect(jsonPath("metadata.name").value("rapport.pdf"))
                .andExpect(jsonPath("metadata.size").value(7)).andExpect(jsonPath("metadata.tags").isEmpty())
                .andReturn().getResponse();
        var metadata = mapper.readTree(response.getContentAsString()).get("metadata");
        Path stored = uploadDirectory.resolve(metadata.get("id").asText());
        assertThat(Files.readString(stored)).isEqualTo("contenu");
        var entity = files.findById(UUID.fromString(metadata.get("id").asText())).orElseThrow();
        assertThat(entity.getOwner()).isNull();
        assertThat(entity.getExpiresAt()).isEqualTo(entity.getUploadedAt().plusDays(7));
        assertThat(metadata.get("downloadLink").asText()).endsWith("/shares/" + entity.getShareToken());
        assertThat(entity.getShareToken()).hasSize(43);
    }

    @Test
    void authenticatedUploadLinksOwnerAndHashesFilePassword() throws Exception {
        String token = register("alice@example.com");
        mvc.perform(multipart("/files").file(document()).param("expirationDays", "1")
                        .param("password", "secret123").param("tags", "projet", "prive")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andExpect(jsonPath("metadata.tags[0]").value("projet"))
                .andExpect(jsonPath("metadata.passwordHash").doesNotExist());
        var entity = files.findAll().getFirst();
        assertThat(entity.getOwner().getId()).isEqualTo(users.findByEmail("alice@example.com").orElseThrow().getId());
        assertThat(entity.getExpiresAt()).isEqualTo(entity.getUploadedAt().plusDays(1));
        assertThat(passwords.matches("secret123", entity.getPasswordFile())).isTrue();
    }

    @Test
    void malformedExpiredWrongIssuerAndTamperedTokensAreRejectedEvenOnPublicUpload() throws Exception {
        String token = register("alice@example.com");
        String subject = jwt.extractEmail(token);
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "." + (parts[2].startsWith("a") ? "b" : "a") + parts[2].substring(1);
        for (String invalid : List.of("invalid", tampered,
                signed(subject, ISSUER, Instant.now().minusSeconds(120)),
                signed(subject, "other-issuer", Instant.now().plusSeconds(3600)))) {
            mvc.perform(multipart("/files").file(document()).header("Authorization", "Bearer " + invalid))
                    .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"))
                    .andExpect(jsonPath("code").value(401));
        }
        assertThat(files.count()).isZero();
        assertNoStoredBytes();
    }

    @Test
    void unknownJwtSubjectCannotOwnFiles() throws Exception {
        mvc.perform(multipart("/files").file(document()).header("Authorization", "Bearer " +
                        signed("missing@example.com", ISSUER, Instant.now().plusSeconds(3600))))
                .andExpect(status().isUnauthorized());
        assertThat(files.count()).isZero();
    }

    @Test
    void protectedRoutesAndCorsAreConfigured() throws Exception {
        mvc.perform(get("/files")).andExpect(status().isUnauthorized());
        mvc.perform(options("/files").header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void swaggerDefinesBearerAuthAndOptionalUploadAuthentication() throws Exception {
        var result = mvc.perform(get("/api-docs")).andExpect(status().isOk()).andReturn();
        var schema = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(schema.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
        var security = schema.at("/paths/~1files/post/security");
        assertThat(security.isArray()).isTrue();
        boolean allowsAnonymous = false;
        for (var requirement : security) { if (requirement.isEmpty()) { allowsAnonymous = true; } }
        assertThat(allowsAnonymous).isTrue();
    }

    @Test
    void signedTokenWithoutExpiryIsRejected() throws Exception {
        String token = Jwts.builder().issuer(ISSUER).subject("alice@example.com")
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(signingSecret)), Jwts.SIG.HS256).compact();
        mvc.perform(multipart("/files").file(document()).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertThat(files.count()).isZero();
    }

    @Test
    void invalidUploadOptionsAreRejectedBeforeWriting() throws Exception {
        for (String days : List.of("0", "8", "not-a-number")) {
            mvc.perform(multipart("/files").file(document()).param("expirationDays", days))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(multipart("/files").file(document()).param("password", "short"))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/files").file(document()).param("tags", "anonymous-tag"))
                .andExpect(status().isBadRequest());
        String token = register("alice@example.com");
        for (String[] tags : List.of(new String[]{"x", " x "}, new String[]{"x".repeat(31)}, new String[]{" "})) {
            mvc.perform(multipart("/files").file(document()).param("tags", tags)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }
        assertThat(files.count()).isZero();
        assertNoStoredBytes();
    }

    @Test
    void missingEmptyBlockedUnsafeAndOversizedFilesAreRejected() throws Exception {
        mvc.perform(multipart("/files")).andExpect(status().isBadRequest());
        mvc.perform(multipart("/files").file(new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0])))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/files").file(new MockMultipartFile("file", "malware.EXE", null, new byte[]{1})))
                .andExpect(status().isUnsupportedMediaType());
        for (String name : List.of("../evil.pdf", "C:\\evil.pdf", "evil.exe.", "evil.exe ")) {
            mvc.perform(multipart("/files").file(new MockMultipartFile("file", name, null, new byte[]{1})))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(multipart("/files").file(new MockMultipartFile("file", "large.pdf", null, new byte[1025])))
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("code").value(413));
        assertThat(files.count()).isZero();
        assertNoStoredBytes();
    }

    private String register(String email) throws Exception {
        var response = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(email, "StrongPass123")))
                .andExpect(status().isCreated()).andExpect(jsonPath("password").doesNotExist())
                .andReturn().getResponse();
        return mapper.readTree(response.getContentAsString()).get("token").asText();
    }

    private String json(String email, String password) throws Exception {
        return mapper.writeValueAsString(Map.of("email", email, "password", password));
    }

    private MockMultipartFile document() {
        return new MockMultipartFile("file", "rapport.pdf", "application/pdf", "contenu".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String signed(String subject, String issuer, Instant expiration) {
        return Jwts.builder().issuer(issuer).subject(subject).issuedAt(Date.from(expiration.minusSeconds(3600)))
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(signingSecret)), Jwts.SIG.HS256).compact();
    }

    private void assertNoStoredBytes() throws Exception {
        try (var paths = Files.list(uploadDirectory)) { assertThat(paths).isEmpty(); }
    }
}
