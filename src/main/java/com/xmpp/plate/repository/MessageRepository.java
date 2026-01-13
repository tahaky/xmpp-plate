package com.xmpp.plate.repository;

import com.xmpp.plate.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    List<Message> findByFromPlateNumberOrToPlateNumberOrderByTimestampDesc(
        String fromPlateNumber, String toPlateNumber
    );
    
    List<Message> findByFromPlateNumberAndToPlateNumberOrToPlateNumberAndFromPlateNumberOrderByTimestampAsc(
        String fromPlate1, String toPlate1, String fromPlate2, String toPlate2
    );
    
    List<Message> findByToPlateNumberAndIsReadFalse(String toPlateNumber);
    
    long countByToPlateNumberAndIsReadFalse(String toPlateNumber);
    
    List<Message> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
