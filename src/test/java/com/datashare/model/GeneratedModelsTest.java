package com.datashare.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedModelsTest {
    @Test
    void authenticationModelsSupportFluentAndBeanAccessors() {
        var login = new LoginRequest().email("alice@example.com").password("secret");
        assertThat(login.getEmail()).isEqualTo("alice@example.com");
        assertThat(login.getPassword()).isEqualTo("secret");
        login.setEmail("bob@example.com");
        login.setPassword("another");
        assertThat(login).isEqualTo(new LoginRequest().email("bob@example.com").password("another"));
        assertThat(login.hashCode()).isEqualTo(new LoginRequest().email("bob@example.com").password("another").hashCode());
        assertThat(login.toString()).contains("bob@example.com");

        var register = new RegisterRequest();
        register.setEmail("new@example.com");
        register.setPassword("password");
        assertThat(register.getEmail()).isEqualTo("new@example.com");
        assertThat(register.getPassword()).isEqualTo("password");
        assertThat(register).isNotEqualTo(login);

        var response = new AuthResponse().token("jwt");
        response.setToken("jwt-2");
        assertThat(response.getToken()).isEqualTo("jwt-2");
        assertThat(response.toString()).contains("[REDACTED]");
    }

    @Test
    void fileAndShareModelsExposeAllFieldsAndCollectionHelpers() {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        var metadata = new FileMetadata().id(id).name("report.pdf").size(12L)
                .uploadDate(now).expirationDate(now.plusDays(1))
                .linkStatus(FileMetadata.LinkStatusEnum.ACTIVE)
                .tags(new ArrayList<>(List.of("project"))).downloadLink("/shares/token");
        assertThat(metadata.getId()).isEqualTo(id);
        assertThat(metadata.getName()).isEqualTo("report.pdf");
        assertThat(metadata.getSize()).isEqualTo(12L);
        assertThat(metadata.getUploadDate()).isEqualTo(now);
        assertThat(metadata.getExpirationDate()).isEqualTo(now.plusDays(1));
        assertThat(metadata.getLinkStatus()).isEqualTo(FileMetadata.LinkStatusEnum.ACTIVE);
        assertThat(metadata.getTags()).containsExactly("project");
        assertThat(metadata.getDownloadLink()).isEqualTo("/shares/token");
        metadata.setTags(new ArrayList<>(List.of("updated")));
        metadata.addTagsItem("second");
        assertThat(metadata.getTags()).containsExactly("updated", "second");
        assertThat(FileMetadata.LinkStatusEnum.fromValue("expired")).isEqualTo(FileMetadata.LinkStatusEnum.EXPIRED);
        assertThat(FileMetadata.LinkStatusEnum.fromValue("unknown")).isNull();
        assertThat(metadata.toString()).contains("report.pdf");

        var share = new ShareMetadata().name("report.pdf").mimeType("application/pdf").size(12L)
                .expirationDate(now).passwordRequired(true);
        assertThat(share.getName()).isEqualTo("report.pdf");
        assertThat(share.getMimeType()).isEqualTo("application/pdf");
        assertThat(share.getSize()).isEqualTo(12L);
        assertThat(share.getExpirationDate()).isEqualTo(now);
        assertThat(share.isPasswordRequired()).isTrue();
        share.setPasswordRequired(false);
        assertThat(share.isPasswordRequired()).isFalse();
        assertThat(share.toString()).contains("report.pdf");

        var upload = new UploadResponse().metadata(metadata);
        assertThat(upload.getMetadata()).isEqualTo(metadata);
        upload.setMetadata(null);
        assertThat(upload.getMetadata()).isNull();
    }

    @Test
    void errorAndRequestModelsSupportAccessorsEqualityAndFormatting() {
        var field = new FieldError().field("email").message("invalid");
        field.setField("password");
        field.setMessage("required");
        assertThat(field.getField()).isEqualTo("password");
        assertThat(field.getMessage()).isEqualTo("required");

        OffsetDateTime timestamp = OffsetDateTime.now();
        var error = new ApiError().code(400).message("invalid").timestamp(timestamp)
                .fieldErrors(new ArrayList<>(List.of(field)));
        error.addFieldErrorsItem(new FieldError().field("file").message("required"));
        assertThat(error.getCode()).isEqualTo(400);
        assertThat(error.getMessage()).isEqualTo("invalid");
        assertThat(error.getTimestamp()).isEqualTo(timestamp);
        assertThat(error.getFieldErrors()).hasSize(2);
        assertThat(error.toString()).contains("invalid");

        var update = new UpdateTagsRequest().tags(new ArrayList<>(List.of("one")));
        update.addTagsItem("two");
        assertThat(update.getTags()).containsExactly("one", "two");
        update.setTags(List.of("three"));
        assertThat(update.getTags()).containsExactly("three");

        var resource = new ByteArrayResource(new byte[]{1, 2});
        var body = new FilesBody().file(resource).password("secret").expirationDays(3)
                .tags(new ArrayList<>(List.of("one")));
        body.addTagsItem("two");
        assertThat(body.getFile()).isSameAs(resource);
        assertThat(body.getPassword()).isEqualTo("secret");
        assertThat(body.getExpirationDays()).isEqualTo(3);
        assertThat(body.getTags()).containsExactly("one", "two");
        body.setFile(null);
        body.setPassword(null);
        body.setExpirationDays(7);
        body.setTags(List.of());
        assertThat(body.getFile()).isNull();
        assertThat(body.toString()).contains("expirationDays: 7");

        var download = new DownloadRequest().password("secret");
        download.setPassword("changed");
        assertThat(download.getPassword()).isEqualTo("changed");
        assertThat(download.toString()).contains("changed");
    }

    @Test
    void generatedModelsImplementEqualityHashCodeAndToStringContracts() {
        assertContract(new AuthResponse().token("token"), new AuthResponse().token("token"),
                new AuthResponse().token("other"));
        assertContract(new DownloadRequest().password("password"), new DownloadRequest().password("password"),
                new DownloadRequest().password("other"));
        assertContract(new RegisterRequest().email("a@example.com").password("password"),
                new RegisterRequest().email("a@example.com").password("password"),
                new RegisterRequest().email("b@example.com").password("password"));
        assertContract(new LoginRequest().email("a@example.com").password("password"),
                new LoginRequest().email("a@example.com").password("password"),
                new LoginRequest().email("b@example.com").password("password"));
        assertContract(new FieldError().field("email").message("invalid"),
                new FieldError().field("email").message("invalid"),
                new FieldError().field("name").message("invalid"));
        assertContract(new UpdateTagsRequest().tags(new ArrayList<>(List.of("tag"))),
                new UpdateTagsRequest().tags(new ArrayList<>(List.of("tag"))),
                new UpdateTagsRequest().tags(new ArrayList<>(List.of("other"))));
        assertContract(new FilesBody().password("password").expirationDays(2)
                        .tags(new ArrayList<>(List.of("tag"))),
                new FilesBody().password("password").expirationDays(2)
                        .tags(new ArrayList<>(List.of("tag"))),
                new FilesBody().password("other").expirationDays(2)
                        .tags(new ArrayList<>(List.of("tag"))));

        var timestamp = OffsetDateTime.now();
        assertContract(new ApiError().code(400).message("bad").timestamp(timestamp)
                        .fieldErrors(new ArrayList<>()),
                new ApiError().code(400).message("bad").timestamp(timestamp)
                        .fieldErrors(new ArrayList<>()),
                new ApiError().code(404).message("missing").timestamp(timestamp)
                        .fieldErrors(new ArrayList<>()));
        assertContract(new ShareMetadata().name("file").mimeType("text/plain").size(1L)
                        .expirationDate(timestamp).passwordRequired(false),
                new ShareMetadata().name("file").mimeType("text/plain").size(1L)
                        .expirationDate(timestamp).passwordRequired(false),
                new ShareMetadata().name("other").mimeType("text/plain").size(1L)
                        .expirationDate(timestamp).passwordRequired(false));
        assertContract(new UploadResponse().metadata(new FileMetadata().name("file")),
                new UploadResponse().metadata(new FileMetadata().name("file")),
                new UploadResponse().metadata(new FileMetadata().name("other")));
        assertThat(FileMetadata.LinkStatusEnum.fromValue("active").toString()).isEqualTo("active");
    }

    private void assertContract(Object value, Object equalValue, Object differentValue) {
        assertThat(value).isEqualTo(value);
        assertThat(value).isEqualTo(equalValue);
        assertThat(value).isNotEqualTo(differentValue);
        assertThat(value.equals(null)).isFalse();
        assertThat(value.equals("not-a-model")).isFalse();
        assertThat(value.hashCode()).isEqualTo(equalValue.hashCode());
        assertThat(value.toString()).isNotBlank();
    }
}
