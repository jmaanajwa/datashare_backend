package com.datashare.controller;

import com.datashare.model.FileMetadata;
import com.datashare.model.UpdateTagsRequest;
import com.datashare.model.UploadResponse;
import com.datashare.service.FileUploadService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class FilesApiController implements FilesApi {

    private final FileUploadService fileUploadService;

    @Override
    public ResponseEntity<UploadResponse> uploadFile(MultipartFile file, String password,
                                                    Integer expirationDays, List<String> tags) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileUploadService.upload(file, password, expirationDays, tags, authentication));
    }

    @Override
    public ResponseEntity<Void> deleteUserFile(UUID fileId) {
        fileUploadService.delete(fileId, currentAuthentication());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<FileMetadata>> listUserFiles() {
        return ResponseEntity.ok(fileUploadService.list(currentAuthentication()));
    }

    @Override
    public ResponseEntity<FileMetadata> updateFileTags(UUID fileId, UpdateTagsRequest body) {
        return ResponseEntity.ok(fileUploadService.updateTags(fileId, body.getTags(), currentAuthentication()));
    }

    private static org.springframework.security.core.Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
