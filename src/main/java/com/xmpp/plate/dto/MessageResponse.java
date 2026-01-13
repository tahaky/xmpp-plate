package com.xmpp.plate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for message response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private Long id;
    private String fromPlateNumber;
    private String toPlateNumber;
    private String messageContent;
    private String messageType;
    private LocalDateTime timestamp;
    private Boolean isDelivered;
    private Boolean isRead;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
}
