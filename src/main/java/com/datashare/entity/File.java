package com.datashare.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Entity
@Table(name = "file")
public class File {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "password_file")
    private String password_file;

    @Column(name = "share_token", nullable = false, unique = true, length = 64)
    private String shareToken;

    @ElementCollection
    @CollectionTable(name = "file_tags", joinColumns = @JoinColumn(name = "file_id"))
    @Column(name = "tag", nullable = false, length = 30)
    private List<String> tags = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getOriginalName() {
        return originalName;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getPasswordFile() {
        return password_file;
    }

    public String getShareToken() {
        return shareToken;
    }

    public List<String> getTags() {
        return List.copyOf(tags);
    }

    public void replaceTags(List<String> newTags) {
        tags = new ArrayList<>(newTags);
    }
}
