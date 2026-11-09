package com.datashare.service;

import com.datashare.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/** Stocke les octets sous un UUID genere par le serveur, sans utiliser le nom client comme chemin. */
@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private final Path root;
    private final long maxBytes;

    public FileStorageService(@Value("${app.storage.directory}") String directory,
                              @Value("${app.storage.max-bytes}") long maxBytes) throws IOException {
        if (maxBytes <= 0) { throw new IllegalArgumentException("La taille maximale doit etre positive."); }
        this.root = Path.of(directory).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        Files.createDirectories(root);
    }

    public long store(UUID id, MultipartFile file) {
        if (file.getSize() > maxBytes) { throw tooLarge(); }
        Path target = root.resolve(id.toString());
        boolean created = false;
        // CREATE_NEW evite tout ecrasement d'un fichier existant.
        try {
            OutputStream opened = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW);
            created = true;
            try (OutputStream output = opened; InputStream input = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                long total = 0;
                int read;
                while ((read = input.read(buffer)) != -1) {
                    total += read;
                    if (total > maxBytes) { throw tooLarge(); }
                    output.write(buffer, 0, read);
                }
                if (total == 0) { throw new BusinessException(HttpStatus.BAD_REQUEST, "Le fichier est vide."); }
                return total;
            }
        } catch (IOException | RuntimeException ex) {
            if (created) { delete(id); }
            if (ex instanceof BusinessException business) { throw business; }
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Impossible de stocker le fichier.");
        }
    }

    public void delete(UUID id) {
        try {
            Files.deleteIfExists(root.resolve(id.toString()));
        } catch (IOException ex) {
            log.error("Impossible de nettoyer le fichier {}", id, ex);
        }
    }

    public Resource load(UUID id) {
        Resource resource = new FileSystemResource(root.resolve(id.toString()));
        if (!resource.exists() || !resource.isReadable()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Fichier physique introuvable.");
        }
        return resource;
    }

    private static BusinessException tooLarge() {
        return new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "Fichier trop volumineux.");
    }
}
