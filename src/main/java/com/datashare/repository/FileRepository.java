package com.datashare.repository;

import com.datashare.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<File, UUID> {
    List<File> findByOwnerIdAndExpiresAtAfterOrderByUploadedAtDesc(UUID ownerId, OffsetDateTime now);
    Optional<File> findByShareToken(String shareToken);
}
