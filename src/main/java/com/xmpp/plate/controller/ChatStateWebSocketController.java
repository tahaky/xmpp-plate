package com.xmpp.plate.controller;

import com.xmpp.plate.dto.ChatStateRequest;
import com.xmpp.plate.service.ChatStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for chat state (typing indicator) events
 */
@Controller
@Slf4j
public class ChatStateWebSocketController {

    @Autowired
    private ChatStateService chatStateService;

    /**
     * Handles incoming chat state updates from WebSocket clients
     * Endpoint: /app/chat-state
     */
    @MessageMapping("/chat-state")
    public void handleChatState(@Payload ChatStateRequest request) {
        log.debug("Received chat state update via WebSocket: {} from {} to {}", 
            request.getState(), request.getPlateNumber(), request.getChatWithPlate());
        
        chatStateService.updateChatState(request);
    }
}
