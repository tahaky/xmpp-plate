package com.xmpp.plate.repository;

import com.xmpp.plate.entity.ChatState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatStateRepository extends JpaRepository<ChatState, Long> {
    
    Optional<ChatState> findByPlateNumberAndChatWithPlate(String plateNumber, String chatWithPlate);
    
    void deleteByPlateNumberAndChatWithPlate(String plateNumber, String chatWithPlate);
}
