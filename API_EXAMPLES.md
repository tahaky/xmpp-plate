# API Usage Examples

This document provides examples of how to use the XMPP Vehicle Messaging Platform API.

## Setup

First, make sure the infrastructure is running:

```bash
# Start PostgreSQL and Openfire
docker-compose up -d postgres openfire

# Wait for Openfire to start (about 30 seconds)
# Access Openfire admin console at http://localhost:9090
# Default credentials: admin/admin

# Run the application
mvn spring-boot:run
```

## API Examples

### 1. Register a Vehicle

Register a new vehicle with its plate number:

```bash
curl -X POST http://localhost:8080/api/vehicles \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "plateNumber": "34ABC123"
  }'
```

Response:
```json
{
  "id": 1,
  "userId": "user123",
  "plateNumber": "34ABC123",
  "xmppUsername": "34ABC123",
  "isActive": true,
  "createdAt": "2024-01-13T12:00:00",
  "lastConnectedAt": null
}
```

### 2. Register Another Vehicle

```bash
curl -X POST http://localhost:8080/api/vehicles \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user456",
    "plateNumber": "06XYZ789"
  }'
```

### 3. Send a Message

Send a message from one vehicle to another:

```bash
curl -X POST http://localhost:8080/api/messages/send \
  -H "Content-Type: application/json" \
  -d '{
    "fromPlateNumber": "34ABC123",
    "toPlateNumber": "06XYZ789",
    "messageContent": "Hello! I need to pass on the left.",
    "messageType": "TEXT"
  }'
```

Response:
```json
{
  "id": 1,
  "fromPlateNumber": "34ABC123",
  "toPlateNumber": "06XYZ789",
  "messageContent": "Hello! I need to pass on the left.",
  "messageType": "TEXT",
  "timestamp": "2024-01-13T12:05:00",
  "isDelivered": true,
  "isRead": false,
  "deliveredAt": "2024-01-13T12:05:00",
  "readAt": null
}
```

### 4. Get Message History

Retrieve all messages for a specific vehicle:

```bash
curl http://localhost:8080/api/messages/34ABC123
```

### 5. Get Conversation Between Two Vehicles

```bash
curl http://localhost:8080/api/messages/conversation/34ABC123/06XYZ789
```

### 6. Get All Vehicles

```bash
curl http://localhost:8080/api/vehicles
```

### 7. Get Specific Vehicle

```bash
curl http://localhost:8080/api/vehicles/34ABC123
```

### 8. Mark Message as Read

```bash
curl -X PUT http://localhost:8080/api/messages/1/read
```

### 9. Get Unread Message Count

```bash
curl http://localhost:8080/api/messages/34ABC123/unread-count
```

### 10. Delete a Vehicle

```bash
curl -X DELETE http://localhost:8080/api/vehicles/34ABC123
```

## WebSocket Examples

### JavaScript Client Example

```javascript
// Include SockJS and STOMP libraries
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/bundles/stomp.umd.min.js"></script>

<script>
const plateNumber = "34ABC123";

// Connect to WebSocket
const socket = new SockJS('http://localhost:8080/ws/chat');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // Subscribe to messages
    stompClient.subscribe('/topic/messages/' + plateNumber, function(message) {
        const msg = JSON.parse(message.body);
        console.log('New message:', msg);
        displayMessage(msg);
    });
    
    // Subscribe to typing indicators
    stompClient.subscribe('/topic/chat-state/' + plateNumber, function(chatState) {
        const state = JSON.parse(chatState.body);
        console.log('Chat state:', state);
        updateTypingIndicator(state);
    });
});

// Send typing indicator
function sendTypingIndicator(state) {
    stompClient.send("/app/chat-state", {}, JSON.stringify({
        plateNumber: plateNumber,
        chatWithPlate: "06XYZ789",
        state: state  // "COMPOSING", "PAUSED", "ACTIVE", etc.
    }));
}

// Example: User starts typing
document.getElementById('messageInput').addEventListener('input', function() {
    sendTypingIndicator('COMPOSING');
});

// Example: User stops typing
let typingTimer;
document.getElementById('messageInput').addEventListener('keyup', function() {
    clearTimeout(typingTimer);
    typingTimer = setTimeout(() => {
        sendTypingIndicator('PAUSED');
    }, 3000); // 3 seconds after user stops typing
});
</script>
```

### Python Client Example

```python
import requests
import json

BASE_URL = "http://localhost:8080"

# Register a vehicle
def register_vehicle(user_id, plate_number):
    response = requests.post(
        f"{BASE_URL}/api/vehicles",
        json={
            "userId": user_id,
            "plateNumber": plate_number
        }
    )
    return response.json()

# Send a message
def send_message(from_plate, to_plate, message):
    response = requests.post(
        f"{BASE_URL}/api/messages/send",
        json={
            "fromPlateNumber": from_plate,
            "toPlateNumber": to_plate,
            "messageContent": message,
            "messageType": "TEXT"
        }
    )
    return response.json()

# Get message history
def get_messages(plate_number):
    response = requests.get(f"{BASE_URL}/api/messages/{plate_number}")
    return response.json()

# Example usage
if __name__ == "__main__":
    # Register vehicles
    vehicle1 = register_vehicle("user123", "34ABC123")
    vehicle2 = register_vehicle("user456", "06XYZ789")
    
    print(f"Registered: {vehicle1}")
    print(f"Registered: {vehicle2}")
    
    # Send message
    message = send_message("34ABC123", "06XYZ789", "Hello from Python!")
    print(f"Sent message: {message}")
    
    # Get messages
    messages = get_messages("06XYZ789")
    print(f"Messages: {messages}")
```

## Testing the Chat State (Typing Indicators)

The typing indicator system follows these states:

- **COMPOSING**: User is actively typing
- **PAUSED**: User paused typing (automatically sent after 3 seconds)
- **ACTIVE**: User is actively engaged in the chat
- **INACTIVE**: User stopped engaging with the chat
- **GONE**: User left the chat

The system automatically handles debouncing - when a user sends multiple COMPOSING states, the PAUSED state will only be sent 3 seconds after the last COMPOSING state.

## Turkish Plate Format

The system validates Turkish license plates with the following format:
- 2 digits (province code)
- 1-3 letters
- 2-4 digits

Valid examples:
- `34ABC123` (Istanbul)
- `06XY1234` (Ankara)
- `35A12` (Izmir)

Invalid examples:
- `4ABC123` (only 1 digit for province)
- `34abc123` (lowercase letters)
- `341234` (no letters)
