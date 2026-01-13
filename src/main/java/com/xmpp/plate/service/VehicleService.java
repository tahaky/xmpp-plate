package com.xmpp.plate.service;

import com.xmpp.plate.dto.VehicleRequest;
import com.xmpp.plate.dto.VehicleResponse;
import com.xmpp.plate.entity.VehicleXmppMapping;
import com.xmpp.plate.exception.VehicleAlreadyExistsException;
import com.xmpp.plate.exception.VehicleNotFoundException;
import com.xmpp.plate.repository.VehicleXmppMappingRepository;
import com.xmpp.plate.util.EncryptionUtil;
import com.xmpp.plate.util.PasswordGenerator;
import com.xmpp.plate.config.XmppConnectionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing vehicle registrations
 */
@Service
@Slf4j
public class VehicleService {

    @Autowired
    private VehicleXmppMappingRepository vehicleRepository;

    @Autowired
    private XmppUserService xmppUserService;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Autowired
    private XmppConnectionManager connectionManager;

    /**
     * Registers a new vehicle
     */
    @Transactional
    public VehicleResponse registerVehicle(VehicleRequest request) {
        log.info("Registering vehicle with plate: {}", request.getPlateNumber());

        // Check if plate already exists
        if (vehicleRepository.existsByPlateNumber(request.getPlateNumber())) {
            throw new VehicleAlreadyExistsException(
                "Vehicle with plate number " + request.getPlateNumber() + " already exists"
            );
        }

        // Generate XMPP credentials
        String xmppUsername = request.getPlateNumber();
        String xmppPassword = PasswordGenerator.generatePassword();

        // Create XMPP user
        xmppUserService.createXmppUser(xmppUsername, xmppPassword);

        // Encrypt password
        String encryptedPassword = encryptionUtil.encrypt(xmppPassword);

        // Save to database
        VehicleXmppMapping mapping = VehicleXmppMapping.builder()
                .userId(request.getUserId())
                .plateNumber(request.getPlateNumber())
                .xmppUsername(xmppUsername)
                .xmppPasswordEncrypted(encryptedPassword)
                .isActive(true)
                .build();

        mapping = vehicleRepository.save(mapping);

        log.info("Vehicle registered successfully: {}", request.getPlateNumber());

        return mapToResponse(mapping);
    }

    /**
     * Deletes a vehicle
     */
    @Transactional
    public void deleteVehicle(String plateNumber) {
        log.info("Deleting vehicle with plate: {}", plateNumber);

        VehicleXmppMapping mapping = vehicleRepository.findByPlateNumber(plateNumber)
                .orElseThrow(() -> new VehicleNotFoundException(
                    "Vehicle with plate number " + plateNumber + " not found"
                ));

        // Disconnect XMPP connection
        connectionManager.removeConnection(plateNumber);

        // Delete XMPP user - Note: May require Openfire REST API for proper deletion
        try {
            xmppUserService.deleteXmppUser(mapping.getXmppUsername());
        } catch (Exception e) {
            log.error("Failed to delete XMPP user for plate {}: {}. Manual cleanup may be required.", 
                plateNumber, e.getMessage());
            // Consider: Implement a cleanup queue or alert system for failed deletions
        }

        // Delete from database
        vehicleRepository.delete(mapping);

        log.info("Vehicle deleted successfully: {}", plateNumber);
    }

    /**
     * Gets vehicle by plate number
     */
    public VehicleResponse getVehicle(String plateNumber) {
        VehicleXmppMapping mapping = vehicleRepository.findByPlateNumber(plateNumber)
                .orElseThrow(() -> new VehicleNotFoundException(
                    "Vehicle with plate number " + plateNumber + " not found"
                ));
        return mapToResponse(mapping);
    }

    /**
     * Gets all vehicles
     */
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets decrypted XMPP password for a vehicle
     */
    public String getXmppPassword(String plateNumber) {
        VehicleXmppMapping mapping = vehicleRepository.findByPlateNumber(plateNumber)
                .orElseThrow(() -> new VehicleNotFoundException(
                    "Vehicle with plate number " + plateNumber + " not found"
                ));
        return encryptionUtil.decrypt(mapping.getXmppPasswordEncrypted());
    }

    /**
     * Updates last connected timestamp
     */
    @Transactional
    public void updateLastConnected(String plateNumber) {
        VehicleXmppMapping mapping = vehicleRepository.findByPlateNumber(plateNumber)
                .orElseThrow(() -> new VehicleNotFoundException(
                    "Vehicle with plate number " + plateNumber + " not found"
                ));
        mapping.setLastConnectedAt(LocalDateTime.now());
        vehicleRepository.save(mapping);
    }

    private VehicleResponse mapToResponse(VehicleXmppMapping mapping) {
        return VehicleResponse.builder()
                .id(mapping.getId())
                .userId(mapping.getUserId())
                .plateNumber(mapping.getPlateNumber())
                .xmppUsername(mapping.getXmppUsername())
                .isActive(mapping.getIsActive())
                .createdAt(mapping.getCreatedAt())
                .lastConnectedAt(mapping.getLastConnectedAt())
                .build();
    }
}
