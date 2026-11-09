package com.datashare.service;

import com.datashare.exception.BusinessException;
import com.datashare.service.FileStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.*;

class FileStorageServiceTest {
    @TempDir Path directory;

    @Test
    void streamSizeLimitIsEnforcedEvenIfReportedSizeIsIncorrect() throws Exception {
        var storage = new FileStorageService(directory.toString(), 8);
        var file = new MockMultipartFile("file", "test.pdf", null, new byte[9]) {
            @Override public long getSize() { return 1; }
        };
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> storage.store(id, file)).isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getStatus().value()).isEqualTo(413));
        assertThat(directory.resolve(id.toString())).doesNotExist();
    }

    @Test
    void inputFailureRemovesPartialFile() throws Exception {
        var storage = new FileStorageService(directory.toString(), 8);
        var file = new MockMultipartFile("file", "test.pdf", null, new byte[]{1}) {
            @Override public InputStream getInputStream() throws IOException { throw new IOException("read failure"); }
        };
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> storage.store(id, file)).isInstanceOf(BusinessException.class);
        assertThat(directory.resolve(id.toString())).doesNotExist();
    }

    @Test
    void existingFileIsNeverOverwrittenOrDeleted() throws Exception {
        var storage = new FileStorageService(directory.toString(), 8);
        UUID id = UUID.randomUUID();
        Files.writeString(directory.resolve(id.toString()), "original");
        assertThatThrownBy(() -> storage.store(id, new MockMultipartFile("file", new byte[]{1})))
                .isInstanceOf(BusinessException.class);
        assertThat(Files.readString(directory.resolve(id.toString()))).isEqualTo("original");
    }
}
