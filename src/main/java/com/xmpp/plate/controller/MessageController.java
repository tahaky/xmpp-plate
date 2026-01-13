package com.xmpp.plate.controller;

import com.xmpp.plate.dto.MessageRequest;
import com.xmpp.plate.dto.MessageResponse;
import com.xmpp.plate.service.MessageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for message operations
 */
@RestController
@RequestMapping("/api/messages")
@Slf4j
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * Send a message
     * POST /api/messages/send
     */
    @PostMapping("/send")
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest request) {
        log.info("Received request to send message from {} to {}", 
            request.getFromPlateNumber(), request.getToPlateNumber());
        MessageResponse response = messageService.sendMessage(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get message history for a plate
     * GET /api/messages/{plateNumber}
     */
    @GetMapping("/{plateNumber}")
    public ResponseEntity<List<MessageResponse>> getMessageHistory(@PathVariable String plateNumber) {
        log.info("Received request to get message history for plate: {}", plateNumber);
        List<MessageResponse> responses = messageService.getMessageHistory(plateNumber);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get conversation between two plates
     * GET /api/messages/conversation/{plate1}/{plate2}
     */
    @GetMapping("/conversation/{plate1}/{plate2}")
    public ResponseEntity<List<MessageResponse>> getConversation(
            @PathVariable String plate1,
            @PathVariable String plate2) {
        log.info("Received request to get conversation between {} and {}", plate1, plate2);
        List<MessageResponse> responses = messageService.getConversation(plate1, plate2);
        return ResponseEntity.ok(responses);
    }

    /**
     * Mark a message as read
     * PUT /api/messages/{messageId}/read
     */
    @PutMapping("/{messageId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long messageId) {
        log.info("Received request to mark message {} as read", messageId);
        messageService.markAsRead(messageId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get unread message count
     * GET /api/messages/{plateNumber}/unread-count
     */
    @GetMapping("/{plateNumber}/unread-count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable String plateNumber) {
        log.info("Received request to get unread count for plate: {}", plateNumber);
        long count = messageService.getUnreadCount(plateNumber);
        return ResponseEntity.ok(count);
    }
}
