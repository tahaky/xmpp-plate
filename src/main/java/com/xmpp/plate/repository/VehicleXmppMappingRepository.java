package com.xmpp.plate.repository;

import com.xmpp.plate.entity.VehicleXmppMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleXmppMappingRepository extends JpaRepository<VehicleXmppMapping, Long> {
    
    Optional<VehicleXmppMapping> findByPlateNumber(String plateNumber);
    
    Optional<VehicleXmppMapping> findByXmppUsername(String xmppUsername);
    
    Optional<VehicleXmppMapping> findByUserId(String userId);
    
    boolean existsByPlateNumber(String plateNumber);
    
    void deleteByPlateNumber(String plateNumber);
}
