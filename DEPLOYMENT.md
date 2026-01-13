# Production Deployment Guide

This guide provides instructions for deploying the XMPP Vehicle Messaging Platform to production.

## Prerequisites

- Java 17 JDK
- PostgreSQL 13+
- Openfire XMPP Server 4.7+
- Docker and Docker Compose (optional)
- 2GB+ RAM
- SSL/TLS certificates (for production)

## Security Checklist

Before deploying to production, ensure you have addressed these security requirements:

### 1. Environment Variables

Replace hardcoded secrets with environment variables:

```bash
export DATABASE_URL="jdbc:postgresql://your-db-host:5432/xmpp_plate"
export DATABASE_USERNAME="your_db_user"
export DATABASE_PASSWORD="your_secure_db_password"
export ENCRYPTION_SECRET_KEY="your_32_character_secret_key_here"
export XMPP_HOST="your-xmpp-server"
export XMPP_ADMIN_USERNAME="admin"
export XMPP_ADMIN_PASSWORD="your_secure_admin_password"
```

Update `application.properties`:
```properties
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}
encryption.secret.key=${ENCRYPTION_SECRET_KEY}
xmpp.host=${XMPP_HOST}
xmpp.admin.username=${XMPP_ADMIN_USERNAME}
xmpp.admin.password=${XMPP_ADMIN_PASSWORD}
```

### 2. Enable TLS for XMPP

In `XmppConnectionManager.java`, change:
```java
.setSecurityMode(ConnectionConfiguration.SecurityMode.required)
```

Configure Openfire to use SSL/TLS certificates.

### 3. Database Security

- Use strong passwords
- Enable SSL for database connections
- Restrict database access by IP
- Regular backups
- Use read replicas if needed

### 4. Disable Debug Logging

In production `application.properties`:
```properties
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
logging.level.root=WARN
logging.level.com.xmpp.plate=INFO
logging.level.org.jivesoftware.smack=WARN
```

### 5. API Security

Consider adding:
- API Gateway
- Rate limiting
- Authentication/Authorization (OAuth2, JWT)
- Input sanitization
- CORS configuration

## Deployment Steps

### Option 1: Docker Deployment (Recommended)

1. **Build the application**:
```bash
mvn clean package -DskipTests
```

2. **Update docker-compose.yml** with production values:
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: xmpp_plate
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - xmpp-network

  openfire:
    image: nasqueron/openfire:latest
    environment:
      - OPENFIRE_ADMIN_USER=${XMPP_ADMIN_USER}
      - OPENFIRE_ADMIN_PASSWORD=${XMPP_ADMIN_PASSWORD}
    ports:
      - "5222:5222"
      - "9090:9090"
    volumes:
      - openfire_data:/var/lib/openfire
    networks:
      - xmpp-network

  app:
    image: xmpp-plate:1.0.0
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/xmpp_plate
      - SPRING_DATASOURCE_USERNAME=${DB_USER}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - ENCRYPTION_SECRET_KEY=${ENCRYPTION_SECRET_KEY}
      - XMPP_HOST=openfire
      - XMPP_ADMIN_USERNAME=${XMPP_ADMIN_USER}
      - XMPP_ADMIN_PASSWORD=${XMPP_ADMIN_PASSWORD}
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - openfire
    networks:
      - xmpp-network

volumes:
  postgres_data:
  openfire_data:

networks:
  xmpp-network:
    driver: bridge
```

3. **Deploy**:
```bash
docker-compose up -d
```

### Option 2: Traditional Deployment

1. **Setup PostgreSQL**:
```sql
CREATE DATABASE xmpp_plate;
CREATE USER xmpp_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE xmpp_plate TO xmpp_user;
```

2. **Install and Configure Openfire**:
- Download from https://www.igniterealtime.org/projects/openfire/
- Run setup wizard
- Configure database connection
- Create admin account
- Enable REST API plugin (optional, but recommended)

3. **Deploy Application**:
```bash
# Build
mvn clean package -DskipTests

# Run with production profile
java -jar target/xmpp-plate-1.0.0.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=${DATABASE_URL} \
  --spring.datasource.username=${DB_USER} \
  --spring.datasource.password=${DB_PASSWORD} \
  --encryption.secret.key=${ENCRYPTION_SECRET_KEY}
```

## Monitoring

### Health Check Endpoints

Add Spring Boot Actuator for health checks:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

Endpoints:
- `GET /actuator/health` - Application health
- `GET /actuator/metrics` - Application metrics

### Log Monitoring

Configure log aggregation:
- ELK Stack (Elasticsearch, Logstash, Kibana)
- Splunk
- CloudWatch (AWS)
- Stackdriver (GCP)

### Alerts

Set up alerts for:
- High error rates
- Database connection failures
- XMPP connection issues
- High memory/CPU usage
- Disk space

## Performance Tuning

### JVM Options

```bash
java -Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -jar target/xmpp-plate-1.0.0.jar
```

### Database Connection Pool

In `application.properties`:
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### XMPP Connection Pool

Adjust based on expected load:
```properties
xmpp.connection.pool.size=50
xmpp.connection.timeout=60000
```

## Backup Strategy

### Database Backups

```bash
# Daily backup
pg_dump -U xmpp_user xmpp_plate > backup_$(date +%Y%m%d).sql

# Automated with cron
0 2 * * * /usr/bin/pg_dump -U xmpp_user xmpp_plate > /backups/xmpp_plate_$(date +\%Y\%m\%d).sql
```

### Openfire Backups

- Backup `/var/lib/openfire` directory
- Export user accounts regularly
- Document configuration changes

## Scaling

### Horizontal Scaling

For high load scenarios:

1. **Load Balancer**: Use Nginx or HAProxy
2. **Multiple App Instances**: Run multiple instances of the application
3. **Shared XMPP Server**: All instances connect to same Openfire cluster
4. **Database Read Replicas**: For read-heavy workloads
5. **Redis Cache**: Cache frequently accessed data

### Vertical Scaling

- Increase server resources (CPU, RAM)
- Optimize database queries
- Implement caching strategies

## Troubleshooting

### Common Issues

1. **XMPP Connection Failures**:
   - Check Openfire is running
   - Verify credentials
   - Check firewall rules (port 5222)

2. **Database Connection Issues**:
   - Verify PostgreSQL is running
   - Check connection string
   - Verify user permissions

3. **WebSocket Not Working**:
   - Check CORS configuration
   - Verify WebSocket endpoint
   - Check proxy/firewall settings

### Logs Location

- Application logs: `logs/xmpp-plate.log`
- Openfire logs: `/var/log/openfire/`
- PostgreSQL logs: `/var/log/postgresql/`

## Openfire REST API (Recommended for Production)

For production use, install Openfire REST API plugin:

1. Access Openfire Admin Console (http://your-server:9090)
2. Go to Plugins → Available Plugins
3. Install "REST API" plugin
4. Configure authentication

Update `XmppUserService.java` to use REST API:

```java
// Example user creation via REST API
POST http://openfire-server:9090/plugins/restapi/v1/users
Authorization: Basic base64(username:password)
Content-Type: application/json

{
  "username": "34ABC123",
  "password": "generated_password",
  "name": "Vehicle 34ABC123"
}
```

## Maintenance

### Regular Tasks

- Monitor disk space
- Review logs weekly
- Update dependencies monthly
- Security patches ASAP
- Database optimization quarterly
- Review and rotate encryption keys yearly

### Update Procedure

1. Backup database and configuration
2. Test updates in staging environment
3. Schedule maintenance window
4. Deploy updates
5. Verify functionality
6. Monitor for issues

## Support

For issues or questions:
- Check logs first
- Review documentation
- Create GitHub issue
- Contact support team

## License

See LICENSE file for details.
