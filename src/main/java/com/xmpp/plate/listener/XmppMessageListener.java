package com.xmpp.plate.listener;

import com.xmpp.plate.dto.MessageResponse;
import com.xmpp.plate.entity.Message;
import com.xmpp.plate.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jxmpp.jid.EntityBareJid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Listener for incoming XMPP messages
 */
@Component
@Slf4j
public class XmppMessageListener implements IncomingChatMessageListener {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void newIncomingMessage(EntityBareJid from, org.jivesoftware.smack.packet.Message message, Chat chat) {
        try {
            log.info("Received message from: {}", from.toString());

            // Extract plate numbers from JIDs
            String fromPlate = from.getLocalpart().toString();
            String toPlate = chat.getXmppAddressOfChatPartner().getLocalpart().toString();

            // Save message to database
            Message savedMessage = Message.builder()
                    .fromPlateNumber(fromPlate)
                    .toPlateNumber(toPlate)
                    .messageContent(message.getBody())
                    .messageType("TEXT")
                    .timestamp(LocalDateTime.now())
                    .isDelivered(true)
                    .deliveredAt(LocalDateTime.now())
                    .xmppMessageId(message.getStanzaId())
                    .build();

            savedMessage = messageRepository.save(savedMessage);

            // Convert to response DTO
            MessageResponse response = MessageResponse.builder()
                    .id(savedMessage.getId())
                    .fromPlateNumber(savedMessage.getFromPlateNumber())
                    .toPlateNumber(savedMessage.getToPlateNumber())
                    .messageContent(savedMessage.getMessageContent())
                    .messageType(savedMessage.getMessageType())
                    .timestamp(savedMessage.getTimestamp())
                    .isDelivered(savedMessage.getIsDelivered())
                    .isRead(savedMessage.getIsRead())
                    .deliveredAt(savedMessage.getDeliveredAt())
                    .readAt(savedMessage.getReadAt())
                    .build();

            // Broadcast to WebSocket subscribers
            messagingTemplate.convertAndSend("/topic/messages/" + toPlate, response);

            log.info("Message processed and broadcasted from {} to {}", fromPlate, toPlate);

        } catch (Exception e) {
            log.error("Error processing incoming message", e);
        }
    }
}
