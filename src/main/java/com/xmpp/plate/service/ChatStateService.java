package com.xmpp.plate.service;

import com.xmpp.plate.config.XmppConnectionManager;
import com.xmpp.plate.dto.ChatStateRequest;
import com.xmpp.plate.entity.ChatState;
import com.xmpp.plate.repository.ChatStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.chatstates.ChatStateManager;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service for handling chat states (typing indicators)
 * Implements debouncing for PAUSED state
 */
@Service
@Slf4j
public class ChatStateService {

    @Autowired
    private ChatStateRepository chatStateRepository;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private XmppConnectionManager connectionManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${xmpp.domain}")
    private String xmppDomain;

    @Value("${typing.indicator.debounce.seconds:3}")
    private int debounceSeconds;

    // Debounce scheduler
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Track pending debounce tasks: plateNumber_chatWithPlate -> ScheduledFuture
    private final Map<String, java.util.concurrent.ScheduledFuture<?>> pendingTasks = new ConcurrentHashMap<>();

    /**
     * Updates chat state with debouncing for COMPOSING->PAUSED transition
     */
    @Transactional
    @Async
    public void updateChatState(ChatStateRequest request) {
        log.debug("Updating chat state: {} from {} to {}", 
            request.getState(), request.getPlateNumber(), request.getChatWithPlate());

        try {
            // Validate both vehicles exist
            vehicleService.getVehicle(request.getPlateNumber());
            vehicleService.getVehicle(request.getChatWithPlate());

            String key = request.getPlateNumber() + "_" + request.getChatWithPlate();

            // Handle COMPOSING state
            if ("COMPOSING".equals(request.getState())) {
                // Cancel any pending PAUSED task
                java.util.concurrent.ScheduledFuture<?> pendingTask = pendingTasks.get(key);
                if (pendingTask != null && !pendingTask.isDone()) {
                    pendingTask.cancel(false);
                }

                // Send COMPOSING state immediately
                sendChatState(request);
                saveChatState(request);

                // Schedule PAUSED state after debounce period
                java.util.concurrent.ScheduledFuture<?> task = scheduler.schedule(() -> {
                    ChatStateRequest pausedRequest = ChatStateRequest.builder()
                            .plateNumber(request.getPlateNumber())
                            .chatWithPlate(request.getChatWithPlate())
                            .state("PAUSED")
                            .build();
                    sendChatState(pausedRequest);
                    saveChatState(pausedRequest);
                }, debounceSeconds, TimeUnit.SECONDS);

                pendingTasks.put(key, task);

            } else {
                // For other states (ACTIVE, PAUSED, INACTIVE, GONE), send immediately
                sendChatState(request);
                saveChatState(request);

                // Cancel any pending tasks
                java.util.concurrent.ScheduledFuture<?> pendingTask = pendingTasks.get(key);
                if (pendingTask != null && !pendingTask.isDone()) {
                    pendingTask.cancel(false);
                    pendingTasks.remove(key);
                }
            }

        } catch (Exception e) {
            log.error("Failed to update chat state", e);
        }
    }

    /**
     * Sends chat state via XMPP
     */
    private void sendChatState(ChatStateRequest request) {
        try {
            // Get XMPP password
            String password = vehicleService.getXmppPassword(request.getPlateNumber());

            // Get XMPP connection
            XMPPTCPConnection connection = connectionManager.getConnection(
                request.getPlateNumber(), 
                password
            );

            // Create recipient JID
            EntityBareJid recipientJid = JidCreate.entityBareFrom(
                request.getChatWithPlate() + "@" + xmppDomain
            );

            // Get chat state manager
            ChatStateManager chatStateManager = ChatStateManager.getInstance(connection);

            // Map and send chat state
            org.jivesoftware.smackx.chatstates.ChatState xmppState = 
                mapToXmppChatState(request.getState());

            if (xmppState != null) {
                // Create a message with chat state
                org.jivesoftware.smack.packet.Message message = 
                    connection.getStanzaFactory()
                        .buildMessageStanza()
                        .to(recipientJid)
                        .build();
                
                // Set chat state on the message
                message.addExtension(new org.jivesoftware.smackx.chatstates.packet.ChatStateExtension(xmppState));
                
                // Send the message with chat state
                connection.sendStanza(message);
                
                log.debug("Sent XMPP chat state {} from {} to {}", 
                    request.getState(), request.getPlateNumber(), request.getChatWithPlate());
            }

            // Broadcast via WebSocket
            messagingTemplate.convertAndSend(
                "/topic/chat-state/" + request.getChatWithPlate(),
                request
            );

        } catch (Exception e) {
            log.error("Failed to send chat state via XMPP", e);
        }
    }

    /**
     * Saves chat state to database
     */
    private void saveChatState(ChatStateRequest request) {
        try {
            ChatState chatState = chatStateRepository
                    .findByPlateNumberAndChatWithPlate(request.getPlateNumber(), request.getChatWithPlate())
                    .orElse(new ChatState());

            chatState.setPlateNumber(request.getPlateNumber());
            chatState.setChatWithPlate(request.getChatWithPlate());
            chatState.setState(request.getState());
            chatState.setTimestamp(LocalDateTime.now());

            chatStateRepository.save(chatState);

        } catch (Exception e) {
            log.error("Failed to save chat state to database", e);
        }
    }

    /**
     * Gets current chat state
     */
    public ChatState getCurrentState(String plateNumber, String chatWithPlate) {
        return chatStateRepository
                .findByPlateNumberAndChatWithPlate(plateNumber, chatWithPlate)
                .orElse(null);
    }

    /**
     * Maps string state to XMPP ChatState enum
     */
    private org.jivesoftware.smackx.chatstates.ChatState mapToXmppChatState(String state) {
        return switch (state) {
            case "ACTIVE" -> org.jivesoftware.smackx.chatstates.ChatState.active;
            case "COMPOSING" -> org.jivesoftware.smackx.chatstates.ChatState.composing;
            case "PAUSED" -> org.jivesoftware.smackx.chatstates.ChatState.paused;
            case "INACTIVE" -> org.jivesoftware.smackx.chatstates.ChatState.inactive;
            case "GONE" -> org.jivesoftware.smackx.chatstates.ChatState.gone;
            default -> null;
        };
    }
}
