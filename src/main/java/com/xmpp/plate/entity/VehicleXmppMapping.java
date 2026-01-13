package com.xmpp.plate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entity for storing Vehicle-XMPP user mapping
 * Maps plate numbers to user IDs and XMPP credentials
 */
@Entity
@Table(name = "vehicle_xmpp_mapping", 
       uniqueConstraints = @UniqueConstraint(columnNames = "plate_number"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleXmppMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "plate_number", nullable = false, unique = true, length = 20)
    private String plateNumber;

    @Column(name = "xmpp_username", nullable = false, unique = true)
    private String xmppUsername;

    @Column(name = "xmpp_password_encrypted", nullable = false, length = 500)
    private String xmppPasswordEncrypted;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_connected_at")
    private LocalDateTime lastConnectedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
