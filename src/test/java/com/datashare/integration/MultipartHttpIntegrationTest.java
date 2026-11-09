package com.datashare.integration;

import com.datashare.PostgresTestConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "debug=false", "logging.level.org.springframework.web=INFO",
        "spring.servlet.multipart.max-file-size=1024B", "spring.servlet.multipart.max-request-size=4096B"})
@ActiveProfiles("test")
@Import(PostgresTestConfiguration.class)
class MultipartHttpIntegrationTest {
    @TempDir static Path uploadDirectory;

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.directory", () -> uploadDirectory.toString());
    }

    @Autowired TestRestTemplate rest;
    @Autowired ObjectMapper mapper;

    @Test
    void realHttpRegistrationAndMultipartWithTextFieldsWork() throws Exception {
        // TestRestTemplate inclut deja le context path /api dans son URI racine.
        assertThat(rest.getRootUri()).endsWith("/api");
        var register = rest.postForEntity("/auth/register",
                Map.of("email", "http@example.com", "password", "StrongPass123"), String.class);
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String token = mapper.readTree(register.getBody()).get("token").asText();
        var form = new LinkedMultiValueMap<String, Object>();
        form.add("file", file(12));
        form.add("expirationDays", "3");
        form.add("tags", "projet");
        form.add("tags", "test");
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);
        var response = rest.postForEntity("/files", new HttpEntity<>(form, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var metadata = mapper.readTree(response.getBody()).get("metadata");
        assertThat(metadata.get("size").asInt()).isEqualTo(12);
        assertThat(metadata.get("tags").get(0).asText()).isEqualTo("projet");
        assertThat(metadata.get("tags").get(1).asText()).isEqualTo("test");
    }

    @Test
    void servletRejectsOversizedMultipartWithApiError() throws Exception {
        var form = new LinkedMultiValueMap<String, Object>();
        form.add("file", file(1025));
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        var response = rest.postForEntity("/files", new HttpEntity<>(form, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(mapper.readTree(response.getBody()).get("code").asInt()).isEqualTo(413);
    }

    private ByteArrayResource file(int size) {
        return new ByteArrayResource(new byte[size]) {
            @Override public String getFilename() { return "rapport.pdf"; }
        };
    }
}
