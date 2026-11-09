package com.datashare.controller;

import com.datashare.exception.GlobalExceptionHandler;
import com.datashare.service.FileUploadService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SharesApiControllerTest {
    private final FileUploadService fileUploadService = mock(FileUploadService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new SharesApiController(fileUploadService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void metadataReturnsPublicShareInformation() throws Exception {
        when(fileUploadService.shareMetadata("token-1"))
                .thenReturn(new FileUploadService.ShareMetadataView(
                        "report.pdf", "application/pdf", 12,
                        java.time.OffsetDateTime.parse("2030-01-01T00:00:00Z"), true));

        mvc.perform(get("/shares/token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("report.pdf"))
                .andExpect(jsonPath("$.mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.passwordRequired").value(true));
    }

    @Test
    void downloadReturnsBinaryContentAndSanitizesFilename() throws Exception {
        var resource = new ByteArrayResource("content".getBytes(StandardCharsets.UTF_8));
        when(fileUploadService.sharedFile("token-1", "1234"))
                .thenReturn(new FileUploadService.SharedFile(resource, "report\".pdf", "application/pdf"));

        mvc.perform(post("/shares/token-1/download").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"1234\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes("content".getBytes(StandardCharsets.UTF_8)))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"report_.pdf\""));
    }
}
