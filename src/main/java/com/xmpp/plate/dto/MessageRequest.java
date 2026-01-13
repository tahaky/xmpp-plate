package com.xmpp.plate.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sending a message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageRequest {

    @NotBlank(message = "From plate number is required")
    private String fromPlateNumber;

    @NotBlank(message = "To plate number is required")
    private String toPlateNumber;

    @NotBlank(message = "Message content is required")
    private String messageContent;

    private String messageType = "TEXT";
}
