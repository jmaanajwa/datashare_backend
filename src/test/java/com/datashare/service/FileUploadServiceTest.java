package com.datashare.service;

import com.datashare.configuration.security.DataShareUserDetails;
import com.datashare.entity.File;
import com.datashare.entity.User;
import com.datashare.exception.BusinessException;
import com.datashare.repository.FileRepository;
import com.datashare.repository.UserRepository;
import com.datashare.service.FileStorageService;
import com.datashare.service.FileUploadService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileUploadServiceTest {
    @TempDir Path directory;

    private final FileRepository files = mock(FileRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final User owner = new User(UUID.randomUUID(), "alice@example.com", "user-hash",
            OffsetDateTime.now(ZoneOffset.UTC));
    private FileUploadService service;
    private Authentication authentication;

    @BeforeEach
    void setUp() throws Exception {
        service = new FileUploadService(files, users, new FileStorageService(directory.toString(), 1024),
                passwords, List.of("exe", "bat"), "http://localhost:8080/api");
        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new DataShareUserDetails(owner));
        when(users.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(passwords.encode("secret123")).thenReturn("file-password-hash");
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void uploadCreatesMetadataAndStoresAuthenticatedOwner() {
        TransactionSynchronizationManager.initSynchronization();

        var response = service.upload(new MockMultipartFile("file", "report.pdf", "application/pdf", "content".getBytes()),
                "secret123", 2, List.of("project", "private"), authentication);

        assertThat(response.getMetadata().getName()).isEqualTo("report.pdf");
        assertThat(response.getMetadata().getTags()).containsExactly("project", "private");
        verify(files).saveAndFlush(any(File.class));
        assertThat(directory).isDirectory();
    }

    @Test
    void listReturnsOnlyActiveFilesForCurrentOwner() {
        var file = file(UUID.randomUUID(), owner, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1), List.of("tag"));
        when(files.findByOwnerIdAndExpiresAtAfterOrderByUploadedAtDesc(eq(owner.getId()), any())).thenReturn(List.of(file));

        var result = service.list(authentication);

        assertThat(result).singleElement().satisfies(metadata -> {
            assertThat(metadata.getId()).isEqualTo(file.getId());
            assertThat(metadata.getLinkStatus()).isEqualTo(com.datashare.model.FileMetadata.LinkStatusEnum.ACTIVE);
        });
    }

    @Test
    void updateTagsAndDeleteRequireOwnership() throws Exception {
        var file = file(UUID.randomUUID(), owner, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1), List.of("old"));
        when(files.findById(file.getId())).thenReturn(Optional.of(file));

        var metadata = service.updateTags(file.getId(), List.of("new", "tags"), authentication);
        assertThat(metadata.getTags()).containsExactly("new", "tags");
        verify(files).saveAndFlush(file);

        Path stored = directory.resolve(file.getId().toString());
        Files.writeString(stored, "content");
        service.delete(file.getId(), authentication);
        verify(files).delete(file);
        assertThat(stored).doesNotExist();
    }

    @Test
    void rejectsMissingOrForeignFilesAndInvalidTags() {
        UUID id = UUID.randomUUID();
        when(files.findById(id)).thenReturn(Optional.empty());
        assertStatus(() -> service.delete(id, authentication), 404);

        var foreign = file(id, new User(UUID.randomUUID(), "other@example.com", "hash", OffsetDateTime.now(ZoneOffset.UTC)),
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(1), List.of());
        when(files.findById(id)).thenReturn(Optional.of(foreign));
        assertStatus(() -> service.updateTags(id, List.of("tag"), authentication), 403);

        var own = file(id, owner, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1), List.of());
        when(files.findById(id)).thenReturn(Optional.of(own));
        assertStatus(() -> service.updateTags(id, List.of("tag", " tag "), authentication), 400);
        assertStatus(() -> service.updateTags(id, List.of("x".repeat(31)), authentication), 400);
    }

    @Test
    void shareMetadataAndDownloadValidateTokenExpiryAndPassword() throws Exception {
        var file = file(UUID.randomUUID(), owner, OffsetDateTime.now(ZoneOffset.UTC).plusDays(1), List.of());
        when(files.findByShareToken("token")).thenReturn(Optional.of(file));
        when(passwords.matches("secret123", "file-password-hash")).thenReturn(true);
        Path stored = directory.resolve(file.getId().toString());
        Files.writeString(stored, "content");

        var metadata = service.shareMetadata("token");
        assertThat(metadata.name()).isEqualTo("document.pdf");
        assertThat(metadata.passwordRequired()).isFalse();

        var shared = service.sharedFile("token", null);
        assertThat(shared.resource().getInputStream().readAllBytes()).containsExactly("content".getBytes());

        var protectedFile = new File(file.getId(), owner, "document.pdf", 7, file.getUploadedAt(), file.getExpiresAt(),
                "file-password-hash", "token", List.of());
        when(files.findByShareToken("protected")).thenReturn(Optional.of(protectedFile));
        assertStatus(() -> service.sharedFile("protected", "wrong"), 403);
        assertThat(service.sharedFile("protected", "secret123").name()).isEqualTo("document.pdf");

        var expired = file(UUID.randomUUID(), owner, OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1), List.of());
        when(files.findByShareToken("expired")).thenReturn(Optional.of(expired));
        assertStatus(() -> service.shareMetadata("expired"), 410);
        assertStatus(() -> service.sharedFile("missing", null), 404);
    }

    private File file(UUID id, User user, OffsetDateTime expiresAt, List<String> tags) {
        return new File(id, user, "document.pdf", 7, OffsetDateTime.now(ZoneOffset.UTC), expiresAt,
                null, "token-" + id, tags);
    }

    private void assertStatus(Runnable action, int status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getStatus().value()).isEqualTo(status));
    }
}
