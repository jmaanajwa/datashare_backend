package com.datashare.service;

import com.datashare.repository.FileRepository;
import com.datashare.repository.UserRepository;
import com.datashare.service.FileStorageService;
import com.datashare.service.FileUploadService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileUploadRollbackTest {
    @TempDir Path directory;

    @Test
    void databaseFailureDeletesUploadedBytesOnRollback() throws Exception {
        var files = mock(FileRepository.class);
        when(files.saveAndFlush(any())).thenThrow(new DataAccessResourceFailureException("database unavailable"));
        var uploads = new FileUploadService(files, mock(UserRepository.class),
                new FileStorageService(directory.toString(), 1024), mock(PasswordEncoder.class),
                List.of("exe"), "http://localhost:8080/api");
        TransactionSynchronizationManager.initSynchronization();
        try {
            assertThatThrownBy(() -> uploads.upload(new MockMultipartFile("file", "test.pdf", null, new byte[]{1}),
                    null, 7, null, null)).isInstanceOf(DataAccessResourceFailureException.class);
            try (var paths = Files.list(directory)) { assertThat(paths.count()).isEqualTo(1); }
            TransactionSynchronizationManager.getSynchronizations().forEach(
                    synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
            try (var paths = Files.list(directory)) { assertThat(paths).isEmpty(); }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
