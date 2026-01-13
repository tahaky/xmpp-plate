package com.xmpp.plate.config;

import lombok.extern.slf4j.Slf4j;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages XMPP connections for all vehicles
 * Maintains a connection pool per plate number
 */
@Component
@Slf4j
public class XmppConnectionManager {

    @Autowired
    private XmppProperties xmppProperties;

    // Connection pool: plateNumber -> XMPPTCPConnection
    private final Map<String, XMPPTCPConnection> connectionPool = new ConcurrentHashMap<>();

    /**
     * Creates and returns an XMPP connection for a vehicle
     */
    public XMPPTCPConnection getConnection(String plateNumber, String password) throws Exception {
        if (connectionPool.containsKey(plateNumber)) {
            XMPPTCPConnection existingConnection = connectionPool.get(plateNumber);
            if (existingConnection.isConnected()) {
                return existingConnection;
            } else {
                // Remove stale connection
                connectionPool.remove(plateNumber);
            }
        }

        // Create new connection
        XMPPTCPConnection connection = createConnection(plateNumber, password);
        connectionPool.put(plateNumber, connection);
        return connection;
    }

    /**
     * Creates a new XMPP connection
     */
    private XMPPTCPConnection createConnection(String plateNumber, String password) throws Exception {
        log.info("Creating XMPP connection for plate: {}", plateNumber);

        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword(plateNumber, password)
                .setXmppDomain(JidCreate.domainBareFrom(xmppProperties.getDomain()))
                .setHost(xmppProperties.getHost())
                .setPort(xmppProperties.getPort())
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .setCompressionEnabled(true)
                .setSendPresence(true)
                .build();

        XMPPTCPConnection connection = new XMPPTCPConnection(config);

        // Enable reconnection
        if (xmppProperties.getConnection().getReconnect().isEnabled()) {
            ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(connection);
            reconnectionManager.enableAutomaticReconnection();
            reconnectionManager.setReconnectionPolicy(ReconnectionManager.ReconnectionPolicy.FIXED_DELAY);
        }

        connection.connect();
        connection.login();

        log.info("Successfully connected to XMPP server for plate: {}", plateNumber);
        return connection;
    }

    /**
     * Disconnects and removes connection for a plate
     */
    public void removeConnection(String plateNumber) {
        XMPPTCPConnection connection = connectionPool.get(plateNumber);
        if (connection != null) {
            try {
                if (connection.isConnected()) {
                    connection.disconnect();
                }
            } catch (Exception e) {
                log.error("Error disconnecting XMPP connection for plate: {}", plateNumber, e);
            } finally {
                connectionPool.remove(plateNumber);
            }
        }
    }

    /**
     * Checks if a connection exists and is active
     */
    public boolean isConnected(String plateNumber) {
        XMPPTCPConnection connection = connectionPool.get(plateNumber);
        return connection != null && connection.isConnected();
    }

    /**
     * Gets admin connection for administrative tasks
     */
    public XMPPTCPConnection getAdminConnection() throws Exception {
        log.info("Creating admin XMPP connection");

        XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword(
                        xmppProperties.getAdmin().getUsername(),
                        xmppProperties.getAdmin().getPassword()
                )
                .setXmppDomain(JidCreate.domainBareFrom(xmppProperties.getDomain()))
                .setHost(xmppProperties.getHost())
                .setPort(xmppProperties.getPort())
                .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                .build();

        XMPPTCPConnection connection = new XMPPTCPConnection(config);
        connection.connect();
        connection.login();

        log.info("Admin connection established");
        return connection;
    }

    /**
     * Disconnects all connections (cleanup)
     */
    public void disconnectAll() {
        log.info("Disconnecting all XMPP connections");
        connectionPool.forEach((plateNumber, connection) -> {
            try {
                if (connection.isConnected()) {
                    connection.disconnect();
                }
            } catch (Exception e) {
                log.error("Error disconnecting connection for plate: {}", plateNumber, e);
            }
        });
        connectionPool.clear();
    }
}
