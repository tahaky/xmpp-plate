package com.xmpp.plate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entity for storing chat state (typing indicators)
 * States: ACTIVE, COMPOSING, PAUSED, INACTIVE, GONE
 */
@Entity
@Table(name = "chat_states", indexes = {
    @Index(name = "idx_plate_chat_state", columnList = "plate_number, chat_with_plate")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plate_number", nullable = false, length = 20)
    private String plateNumber;

    @Column(name = "chat_with_plate", nullable = false, length = 20)
    private String chatWithPlate;

    @Column(name = "state", nullable = false, length = 20)
    private String state; // ACTIVE, COMPOSING, PAUSED, INACTIVE, GONE

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        timestamp = LocalDateTime.now();
    }
}
