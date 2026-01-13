package com.xmpp.plate.service;

import com.xmpp.plate.config.XmppConnectionManager;
import com.xmpp.plate.dto.MessageRequest;
import com.xmpp.plate.dto.MessageResponse;
import com.xmpp.plate.entity.Message;
import com.xmpp.plate.exception.VehicleNotFoundException;
import com.xmpp.plate.exception.XmppOperationException;
import com.xmpp.plate.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for handling message operations
 */
@Service
@Slf4j
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private XmppConnectionManager connectionManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${xmpp.domain}")
    private String xmppDomain;

    /**
     * Sends a message from one vehicle to another
     */
    @Transactional
    public MessageResponse sendMessage(MessageRequest request) {
        log.info("Sending message from {} to {}", request.getFromPlateNumber(), request.getToPlateNumber());

        try {
            // Validate sender exists
            vehicleService.getVehicle(request.getFromPlateNumber());
            
            // Validate recipient exists
            vehicleService.getVehicle(request.getToPlateNumber());

            // Get XMPP password for sender
            String password = vehicleService.getXmppPassword(request.getFromPlateNumber());

            // Get or create XMPP connection
            XMPPTCPConnection connection = connectionManager.getConnection(
                request.getFromPlateNumber(), 
                password
            );

            // Create recipient JID
            EntityBareJid recipientJid = JidCreate.entityBareFrom(
                request.getToPlateNumber() + "@" + xmppDomain
            );

            // Get chat manager and create chat
            ChatManager chatManager = ChatManager.getInstanceFor(connection);
            Chat chat = chatManager.chatWith(recipientJid);

            // Create and send XMPP message
            org.jivesoftware.smack.packet.Message xmppMessage = MessageBuilder.buildMessage()
                    .to(recipientJid)
                    .setBody(request.getMessageContent())
                    .build();

            chat.send(xmppMessage);

            // Save message to database
            Message message = Message.builder()
                    .fromPlateNumber(request.getFromPlateNumber())
                    .toPlateNumber(request.getToPlateNumber())
                    .messageContent(request.getMessageContent())
                    .messageType(request.getMessageType())
                    .timestamp(LocalDateTime.now())
                    .isDelivered(true)
                    .deliveredAt(LocalDateTime.now())
                    .xmppMessageId(xmppMessage.getStanzaId())
                    .build();

            message = messageRepository.save(message);

            log.info("Message sent successfully from {} to {}", request.getFromPlateNumber(), request.getToPlateNumber());

            // Broadcast via WebSocket
            MessageResponse response = mapToResponse(message);
            messagingTemplate.convertAndSend(
                "/topic/messages/" + request.getToPlateNumber(), 
                response
            );

            return response;

        } catch (VehicleNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to send message", e);
            throw new XmppOperationException("Failed to send message: " + e.getMessage(), e);
        }
    }

    /**
     * Gets message history for a plate number
     */
    public List<MessageResponse> getMessageHistory(String plateNumber) {
        log.info("Getting message history for plate: {}", plateNumber);

        // Validate vehicle exists
        vehicleService.getVehicle(plateNumber);

        List<Message> messages = messageRepository
                .findByFromPlateNumberOrToPlateNumberOrderByTimestampDesc(plateNumber, plateNumber);

        return messages.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets conversation between two plates
     */
    public List<MessageResponse> getConversation(String plate1, String plate2) {
        log.info("Getting conversation between {} and {}", plate1, plate2);

        List<Message> messages = messageRepository
                .findByFromPlateNumberAndToPlateNumberOrToPlateNumberAndFromPlateNumberOrderByTimestampAsc(
                    plate1, plate2, plate1, plate2
                );

        return messages.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Marks a message as read
     */
    @Transactional
    public void markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        message.setIsRead(true);
        message.setReadAt(LocalDateTime.now());
        messageRepository.save(message);

        log.info("Message {} marked as read", messageId);
    }

    /**
     * Gets unread message count
     */
    public long getUnreadCount(String plateNumber) {
        return messageRepository.countByToPlateNumberAndIsReadFalse(plateNumber);
    }

    private MessageResponse mapToResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .fromPlateNumber(message.getFromPlateNumber())
                .toPlateNumber(message.getToPlateNumber())
                .messageContent(message.getMessageContent())
                .messageType(message.getMessageType())
                .timestamp(message.getTimestamp())
                .isDelivered(message.getIsDelivered())
                .isRead(message.getIsRead())
                .deliveredAt(message.getDeliveredAt())
                .readAt(message.getReadAt())
                .build();
    }
}
