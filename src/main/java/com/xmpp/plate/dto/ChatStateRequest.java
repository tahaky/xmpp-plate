package com.xmpp.plate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for chat state (typing indicator)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatStateRequest {

    private String plateNumber;
    private String chatWithPlate;
    private String state; // ACTIVE, COMPOSING, PAUSED, INACTIVE, GONE
}
