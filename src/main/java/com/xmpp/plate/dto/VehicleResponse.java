package com.xmpp.plate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for vehicle response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {

    private Long id;
    private String userId;
    private String plateNumber;
    private String xmppUsername;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastConnectedAt;
}
