package com.xmpp.plate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for adding a new vehicle
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Plate number is required")
    @Pattern(regexp = "^\\d{2}[A-Z]{1,3}\\d{2,4}$", 
             message = "Invalid Turkish plate format. Expected format: 34ABC123")
    private String plateNumber;
}
