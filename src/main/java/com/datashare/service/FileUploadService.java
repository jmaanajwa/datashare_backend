package com.datashare.service;

import com.datashare.entity.File;
import com.datashare.entity.User;
import com.datashare.configuration.security.DataShareUserDetails;
import com.datashare.exception.BusinessException;
import com.datashare.model.FileMetadata;
import com.datashare.model.UploadResponse;
import com.datashare.repository.FileRepository;
import com.datashare.repository.UserRepository;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.net.URLConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import static org.springframework.http.HttpStatus.*;

@Service
public class FileUploadService {
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileStorageService storage;
    private final PasswordEncoder passwords;
    private final Set<String> blockedExtensions;
    private final String publicApiUrl;
    private final SecureRandom random = new SecureRandom();

    public FileUploadService(FileRepository fileRepository, UserRepository userRepository, FileStorageService storage,
                             PasswordEncoder passwords,
                             @Value("${app.storage.blocked-extensions}") List<String> blockedExtensions,
                             @Value("${app.public-api-url}") String publicApiUrl) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.storage = storage;
        this.passwords = passwords;
        this.blockedExtensions = new HashSet<>();
        blockedExtensions.forEach(extension -> this.blockedExtensions.add(extension.strip().toLowerCase(Locale.ROOT)));
        this.publicApiUrl = publicApiUrl.replaceAll("/+$", "");
    }


    @Transactional
    public UploadResponse upload(MultipartFile file, String password, Integer expirationDays,
                                 List<String> tags, Authentication authentication) {
        String name = validateFilename(file);
        int days = expirationDays == null ? 7 : expirationDays;
        if (days < 1 || days > 7) { throw invalid("La duree de validite doit etre comprise entre 1 et 7 jours."); }
        User owner = resolveOwner(authentication);
        List<String> validatedTags = validateTags(tags, owner);
        String passwordHash = null;
        if (password != null) {
            AuthService.validate(password, 6);
            passwordHash = passwords.encode(password);
        }
        UUID id = UUID.randomUUID();
        long size = storage.store(id, file);
        // Couvre un echec de sauvegarde ou de commit, apres ecriture sur disque.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) { storage.delete(id); }
            }
        });
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        var entity = new File(id, owner, name, size, now, now.plusDays(days), passwordHash, token, validatedTags);
        fileRepository.saveAndFlush(entity);
        var metadata = new FileMetadata().id(id).name(name).size(size).uploadDate(now)
                .expirationDate(entity.getExpiresAt()).linkStatus(FileMetadata.LinkStatusEnum.ACTIVE)
                .tags(validatedTags).downloadLink(publicApiUrl + "/shares/" + token);
        return new UploadResponse().metadata(metadata);
    }

    @Transactional(readOnly = true)
    public List<FileMetadata> list(Authentication authentication) {
        User owner = authenticatedOwner(authentication);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return fileRepository.findByOwnerIdAndExpiresAtAfterOrderByUploadedAtDesc(owner.getId(), now)
                .stream().map(this::metadata).toList();
    }

    @Transactional
    public void delete(UUID fileId, Authentication authentication) {
        User owner = authenticatedOwner(authentication);
        File file = find(fileId);
        checkOwner(file, owner);
        fileRepository.delete(file);
        storage.delete(file.getId());
    }

    @Transactional
    public FileMetadata updateTags(UUID fileId, List<String> tags, Authentication authentication) {
        User owner = authenticatedOwner(authentication);
        File file = find(fileId);
        checkOwner(file, owner);
        file.replaceTags(validateTags(tags, owner));
        fileRepository.saveAndFlush(file);
        return metadata(file);
    }

    @Transactional(readOnly = true)
    public ShareMetadataView shareMetadata(String token) {
        File file = findByToken(token);
        checkNotExpired(file);
        return new ShareMetadataView(file.getOriginalName(), contentType(file), file.getSizeBytes(),
                file.getExpiresAt(), file.getPasswordFile() != null);
    }

    @Transactional(readOnly = true)
    public SharedFile sharedFile(String token, String password) {
        File file = findByToken(token);
        checkNotExpired(file);
        if (file.getPasswordFile() != null
                && (password == null || !passwords.matches(password, file.getPasswordFile()))) {
            throw new BusinessException(FORBIDDEN, "Mot de passe incorrect.");
        }
        return new SharedFile(storage.load(file.getId()), file.getOriginalName(), contentType(file));
    }

    private FileMetadata metadata(File file) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return new FileMetadata().id(file.getId()).name(file.getOriginalName()).size(file.getSizeBytes())
                .uploadDate(file.getUploadedAt()).expirationDate(file.getExpiresAt())
                .linkStatus(file.getExpiresAt().isAfter(now) ? FileMetadata.LinkStatusEnum.ACTIVE
                        : FileMetadata.LinkStatusEnum.EXPIRED)
                .tags(file.getTags()).downloadLink(publicApiUrl + "/shares/" + file.getShareToken());
    }

    private User authenticatedOwner(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof DataShareUserDetails principal)) {
            throw new BusinessException(UNAUTHORIZED, "Authentification requise.");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException(UNAUTHORIZED, "Utilisateur inconnu."));
    }

    private File find(UUID fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND, "Fichier introuvable."));
    }

    private File findByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(NOT_FOUND, "Lien de partage introuvable.");
        }
        return fileRepository.findByShareToken(token)
                .orElseThrow(() -> new BusinessException(NOT_FOUND, "Lien de partage introuvable."));
    }

    private static void checkOwner(File file, User owner) {
        if (file.getOwner() == null || !file.getOwner().getId().equals(owner.getId())) {
            throw new BusinessException(FORBIDDEN, "Acces interdit.");
        }
    }

    private static void checkNotExpired(File file) {
        if (!file.getExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(GONE, "Lien de partage expire.");
        }
    }

    private static String contentType(File file) {
        String type = URLConnection.guessContentTypeFromName(file.getOriginalName());
        return type == null ? "application/octet-stream" : type;
    }

    public record ShareMetadataView(String name, String contentType, long size,
                                    OffsetDateTime expirationDate, boolean passwordRequired) { }

    public record SharedFile(Resource resource, String name, String contentType) { }

    private User resolveOwner(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof DataShareUserDetails principal)) {
            return null;
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Utilisateur inconnu."));
    }

    private String validateFilename(MultipartFile file) {
        if (file == null || file.isEmpty()) { throw invalid("Le fichier est obligatoire et ne doit pas etre vide."); }
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank() || name.length() > 255 || name.equals(".") || name.equals("..")
                || !name.equals(name.strip()) || name.contains("/") || name.contains("\\")
                || name.contains(":") || name.endsWith(".") || name.chars().anyMatch(Character::isISOControl)) {
            throw invalid("Nom de fichier invalide.");
        }
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (blockedExtensions.contains(extension)) {
            throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Cette extension de fichier est interdite.");
        }
        return name;
    }

    private List<String> validateTags(List<String> tags, User owner) {
        if (tags == null || tags.isEmpty()) { return List.of(); }
        if (owner == null) { throw invalid("Les tags necessitent une authentification."); }
        Set<String> unique = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null || tag.isBlank() || tag.strip().length() > 30 || !unique.add(tag.strip())) {
                throw invalid("Les tags doivent etre uniques, non vides et limites a 30 caracteres.");
            }
        }
        return List.copyOf(unique);
    }

    private static BusinessException invalid(String message) { return new BusinessException(HttpStatus.BAD_REQUEST, message); }
}
