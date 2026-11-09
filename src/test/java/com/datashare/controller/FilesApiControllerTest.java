package com.datashare.controller;

import com.datashare.exception.GlobalExceptionHandler;
import com.datashare.model.FileMetadata;
import com.datashare.model.UpdateTagsRequest;
import com.datashare.model.UploadResponse;
import com.datashare.service.FileUploadService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FilesApiControllerTest {
    private final FileUploadService fileUploadService = mock(FileUploadService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new FilesApiController(fileUploadService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice@example.com", null));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadDelegatesMultipartArgumentsAndReturnsCreated() throws Exception {
        when(fileUploadService.upload(any(), eq("1234"), eq(1), eq(List.of("project", "test")), any()))
                .thenReturn(new UploadResponse().metadata(new FileMetadata().name("report.pdf")));

        mvc.perform(multipart("/files")
                        .file(new MockMultipartFile("file", "report.pdf", "application/pdf", "data".getBytes()))
                        .param("password", "1234")
                        .param("expirationDays", "1")
                        .param("tags", "project", "test"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metadata.name").value("report.pdf"));
    }

    @Test
    void listReturnsCurrentUserFiles() throws Exception {
        when(fileUploadService.list(any())).thenReturn(List.of(new FileMetadata().name("report.pdf").size(4L)));

        mvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("report.pdf"))
                .andExpect(jsonPath("$[0].size").value(4));
    }

    @Test
    void updateTagsDelegatesFileIdAndBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(fileUploadService.updateTags(eq(id), eq(List.of("new", "tags")), any()))
                .thenReturn(new FileMetadata().id(id).name("report.pdf"));

        mvc.perform(put("/files/{fileId}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tags\":[\"new\",\"tags\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mvc.perform(delete("/files/{fileId}", id))
                .andExpect(status().isNoContent());

        verify(fileUploadService).delete(eq(id), any());
    }
}
