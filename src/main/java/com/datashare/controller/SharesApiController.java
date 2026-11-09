package com.datashare.controller;

import com.datashare.model.DownloadRequest;
import org.springframework.core.io.Resource;
import com.datashare.model.ShareMetadata;
import com.datashare.service.FileUploadService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2026-10-22T13:35:09.135659088Z[GMT]")
@RestController
@RequiredArgsConstructor
public class SharesApiController implements SharesApi {

    private final FileUploadService fileUploadService;

    public ResponseEntity<Resource> downloadSharedFile(@Parameter(in = ParameterIn.PATH, description = "Token unique de partage", required=true, schema=@Schema()) @PathVariable("token") String token
,@Parameter(in = ParameterIn.DEFAULT, description = "", schema=@Schema()) @Valid @RequestBody DownloadRequest body
) {
        var file = fileUploadService.sharedFile(token, body == null ? null : body.getPassword());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safeFilename(file.name()) + "\"")
                .body(file.resource());
    }

    public ResponseEntity<ShareMetadata> getShareMetadata(@Parameter(in = ParameterIn.PATH, description = "Token unique de partage", required=true, schema=@Schema()) @PathVariable("token") String token
) {
        var metadata = fileUploadService.shareMetadata(token);
        return ResponseEntity.ok(new ShareMetadata().name(metadata.name()).mimeType(metadata.contentType())
                .size(metadata.size()).expirationDate(metadata.expirationDate())
                .passwordRequired(metadata.passwordRequired()));
    }

    private static String safeFilename(String name) {
        return name.replace("\\", "_").replace("\"", "_").replace("\r", "_").replace("\n", "_");
    }

}
