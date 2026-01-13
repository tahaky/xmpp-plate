# XMPP Vehicle Messaging Platform

A production-ready Spring Boot application for vehicle-to-vehicle messaging using XMPP protocol. This system enables real-time messaging between vehicles identified by their license plate numbers.

## Features

- 🚗 **Vehicle Registration**: Map license plates to XMPP users
- 💬 **Real-time Messaging**: Vehicle-to-vehicle instant messaging
- ⌨️ **Typing Indicators**: Real-time chat state notifications (composing, paused, active, etc.)
- 📜 **Message History**: Persistent message storage with read receipts
- 🔐 **Secure Credentials**: Encrypted XMPP password storage
- 🔄 **Auto-reconnect**: Automatic connection recovery
- 🌐 **WebSocket Support**: Real-time updates via WebSocket
- 🐘 **PostgreSQL**: Reliable data persistence
- 🔥 **Openfire Integration**: Enterprise-grade XMPP server

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Data JPA**
- **Spring WebSocket**
- **Smack 4.4.8** (XMPP Client Library)
- **PostgreSQL**
- **Openfire** (XMPP Server)
- **Docker & Docker Compose**

## Architecture

```
┌─────────────────┐
│  Client App     │
│  (REST/WS)      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌──────────────┐
│  Spring Boot    │────▶│  PostgreSQL  │
│  Application    │     └──────────────┘
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Openfire       │
│  XMPP Server    │
└─────────────────┘
```

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker & Docker Compose (for easy setup)

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/tahaky/xmpp-plate.git
cd xmpp-plate
```

### 2. Start Infrastructure (PostgreSQL + Openfire)

```bash
docker-compose up -d postgres openfire
```

### 3. Configure Openfire

**Important**: Wait for Openfire to fully start (about 30-60 seconds), then:

1. Access the admin console: http://localhost:9090
2. Login with credentials:
   - Username: `admin`
   - Password: `admin`
3. **Enable in-band registration** (required for vehicle registration):
   - Navigate to: **Server** → **Server Settings** → **Registration & Login**
   - Check: **"Enable in-band account registration"**
   - Click: **Save Settings**

### 4. Run the Application

#### Option A: Using Maven
```bash
mvn spring-boot:run
```

#### Option B: Using Docker
```bash
docker-compose up -d app
```

The application will be available at: http://localhost:8080

## API Documentation

### Vehicle Management

#### Register a Vehicle
```bash
POST /api/vehicles
Content-Type: application/json

{
  "userId": "user123",
  "plateNumber": "34ABC123"
}
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

#### Get Vehicle by Plate
```bash
GET /api/vehicles/34ABC123
```

#### Get All Vehicles
```bash
GET /api/vehicles
```

#### Delete Vehicle
```bash
DELETE /api/vehicles/34ABC123
```

### Messaging

#### Send a Message
```bash
POST /api/messages/send
Content-Type: application/json

{
  "fromPlateNumber": "34ABC123",
  "toPlateNumber": "06XYZ789",
  "messageContent": "Hello from vehicle 34ABC123!",
  "messageType": "TEXT"
}
```

#### Get Message History
```bash
GET /api/messages/34ABC123
```

#### Get Conversation Between Two Vehicles
```bash
GET /api/messages/conversation/34ABC123/06XYZ789
```

#### Mark Message as Read
```bash
PUT /api/messages/{messageId}/read
```

#### Get Unread Count
```bash
GET /api/messages/34ABC123/unread-count
```

### WebSocket Integration

#### Connect to WebSocket
```javascript
const socket = new SockJS('http://localhost:8080/ws/chat');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // Subscribe to messages for a specific plate
    stompClient.subscribe('/topic/messages/34ABC123', function(message) {
        console.log('Received message:', JSON.parse(message.body));
    });
    
    // Subscribe to typing indicators
    stompClient.subscribe('/topic/chat-state/34ABC123', function(chatState) {
        console.log('Chat state update:', JSON.parse(chatState.body));
    });
});
```

#### Send Typing Indicator
```javascript
stompClient.send("/app/chat-state", {}, JSON.stringify({
    plateNumber: "34ABC123",
    chatWithPlate: "06XYZ789",
    state: "COMPOSING"  // ACTIVE, COMPOSING, PAUSED, INACTIVE, GONE
}));
```

## Typing Indicator States

- **ACTIVE**: User is actively engaged in the chat
- **COMPOSING**: User is typing a message
- **PAUSED**: User has paused typing (automatically sent after 3 seconds of inactivity)
- **INACTIVE**: User has stopped engaging with the chat
- **GONE**: User has left the chat

## Configuration

Key configuration properties in `application.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/xmpp_plate
spring.datasource.username=postgres
spring.datasource.password=postgres

# XMPP Server
xmpp.host=localhost
xmpp.port=5222
xmpp.domain=localhost
xmpp.admin.username=admin
xmpp.admin.password=admin

# Typing Indicator
typing.indicator.debounce.seconds=3

# Encryption
encryption.secret.key=MySecretKey12345MySecretKey12345
```

## Turkish Plate Format Validation

The system validates Turkish license plates with the format:
- 2 digits + 1-3 letters + 2-4 digits
- Examples: `34ABC123`, `06XY1234`, `35A12`

Regex pattern: `^\d{2}[A-Z]{1,3}\d{2,4}$`

## Security Features

- ✅ Encrypted XMPP credentials (AES-256)
- ✅ Input validation for all API requests
- ✅ No authentication/authorization (delegated to external service)
- ✅ Secure password generation for XMPP accounts

## Error Handling

The application provides comprehensive error handling with structured error responses:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Vehicle with plate number 34ABC123 not found",
  "path": "/api/vehicles/34ABC123",
  "timestamp": "2024-01-13T12:00:00"
}
```

## Logging

Logs are written to:
- Console: Real-time application logs
- File: `logs/xmpp-plate.log`

Log levels can be configured in `application.properties`.

## Development

### Build the Project
```bash
mvn clean package
```

### Run Tests
```bash
mvn test
```

### Build Docker Image
```bash
docker build -t xmpp-plate:latest .
```

## Troubleshooting

### XMPP Authentication Error (SASLError: not-authorized)

This error occurs when XMPP user accounts cannot be created or authenticated. **Solution**:

1. **Enable In-Band Registration in Openfire**:
   - Access Openfire admin console: http://localhost:9090
   - Login with credentials (default: `admin`/`admin`)
   - Navigate to: **Server** → **Server Settings** → **Registration & Login**
   - Check the box: **"Enable in-band account registration"**
   - Click **Save Settings**

2. **Verify Openfire is fully started** (takes 30-60 seconds):
   ```bash
   docker logs xmpp-plate-openfire
   ```
   Wait for "Openfire started" message before registering vehicles.

3. **Clear existing data and retry**:
   ```bash
   docker-compose down -v
   docker-compose up -d
   ```

### Openfire Connection Issues
- Ensure Openfire is running: `docker ps | grep openfire`
- Check Openfire admin console: http://localhost:9090
- Verify XMPP domain matches configuration
- Confirm in-band registration is enabled (see above)

### Database Connection Issues
- Check PostgreSQL is running: `docker ps | grep postgres`
- Verify database credentials in `application.properties`
- Ensure username is `postgres` (not `posrgres` or similar typos)

### Message Not Received
- Ensure both vehicles are registered
- Check XMPP connections are active
- Verify XMPP user accounts exist in Openfire admin console (Users/Groups → Users)
- Review application logs for errors

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Contact

For questions or support, please open an issue on GitHub.

---

Built with ❤️ using Spring Boot and XMPP
