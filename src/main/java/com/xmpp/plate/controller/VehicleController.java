package com.xmpp.plate.controller;

import com.xmpp.plate.dto.VehicleRequest;
import com.xmpp.plate.dto.VehicleResponse;
import com.xmpp.plate.service.VehicleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for vehicle management
 */
@RestController
@RequestMapping("/api/vehicles")
@Slf4j
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    /**
     * Register a new vehicle
     * POST /api/vehicles
     */
    @PostMapping
    public ResponseEntity<VehicleResponse> registerVehicle(@Valid @RequestBody VehicleRequest request) {
        log.info("Received request to register vehicle: {}", request.getPlateNumber());
        VehicleResponse response = vehicleService.registerVehicle(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Delete a vehicle by plate number
     * DELETE /api/vehicles/{plateNumber}
     */
    @DeleteMapping("/{plateNumber}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable String plateNumber) {
        log.info("Received request to delete vehicle: {}", plateNumber);
        vehicleService.deleteVehicle(plateNumber);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get vehicle by plate number
     * GET /api/vehicles/{plateNumber}
     */
    @GetMapping("/{plateNumber}")
    public ResponseEntity<VehicleResponse> getVehicle(@PathVariable String plateNumber) {
        log.info("Received request to get vehicle: {}", plateNumber);
        VehicleResponse response = vehicleService.getVehicle(plateNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all vehicles
     * GET /api/vehicles
     */
    @GetMapping
    public ResponseEntity<List<VehicleResponse>> getAllVehicles() {
        log.info("Received request to get all vehicles");
        List<VehicleResponse> responses = vehicleService.getAllVehicles();
        return ResponseEntity.ok(responses);
    }
}
