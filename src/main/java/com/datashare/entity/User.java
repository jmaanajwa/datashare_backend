package com.datashare.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Entity
@Table(name = "\"user\"")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_user", nullable = false)
    private String password_user;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;



    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordUser() {
        return password_user;
    }
}
