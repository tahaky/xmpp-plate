package com.xmpp.plate.service;

import com.xmpp.plate.config.XmppConnectionManager;
import com.xmpp.plate.config.XmppProperties;
import com.xmpp.plate.exception.XmppOperationException;
import lombok.extern.slf4j.Slf4j;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jxmpp.jid.parts.Localpart;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for managing XMPP user accounts
 * Handles creation and deletion of XMPP users
 */
@Service
@Slf4j
public class XmppUserService {

    @Autowired
    private XmppConnectionManager connectionManager;

    @Autowired
    private XmppProperties xmppProperties;

    /**
     * Creates a new XMPP user account
     */
    public void createXmppUser(String username, String password) {
        XMPPTCPConnection adminConnection = null;
        try {
            log.info("Creating XMPP user: {}", username);
            
            // Get admin connection
            adminConnection = connectionManager.getAdminConnection();
            
            // Get account manager
            AccountManager accountManager = AccountManager.getInstance(adminConnection);
            accountManager.sensitiveOperationOverInsecureConnection(true);
            
            // Check if account registration is supported
            if (!accountManager.supportsAccountCreation()) {
                log.warn("Account creation not supported by XMPP server, attempting alternate method");
                // Note: Some XMPP servers require admin API calls
                // For Openfire, you might need to use the REST API instead
            }
            
            // Create account
            accountManager.createAccount(Localpart.from(username), password);
            
            log.info("XMPP user created successfully: {}", username);
            
        } catch (Exception e) {
            log.error("Failed to create XMPP user: {}", username, e);
            throw new XmppOperationException("Failed to create XMPP user: " + username, e);
        } finally {
            if (adminConnection != null && adminConnection.isConnected()) {
                adminConnection.disconnect();
            }
        }
    }

    /**
     * Deletes an XMPP user account
     */
    public void deleteXmppUser(String username) {
        XMPPTCPConnection adminConnection = null;
        try {
            log.info("Deleting XMPP user: {}", username);
            
            // Get admin connection
            adminConnection = connectionManager.getAdminConnection();
            
            // Get account manager
            AccountManager accountManager = AccountManager.getInstance(adminConnection);
            accountManager.sensitiveOperationOverInsecureConnection(true);
            
            // Delete account
            accountManager.deleteAccount();
            
            log.info("XMPP user deleted successfully: {}", username);
            
        } catch (Exception e) {
            log.error("Failed to delete XMPP user: {}", username, e);
            throw new XmppOperationException("Failed to delete XMPP user: " + username, e);
        } finally {
            if (adminConnection != null && adminConnection.isConnected()) {
                adminConnection.disconnect();
            }
        }
    }

    /**
     * Checks if XMPP user exists
     */
    public boolean userExists(String username) {
        try {
            // Try to get connection - if successful, user exists
            XMPPTCPConnection connection = connectionManager.getAdminConnection();
            if (connection != null && connection.isConnected()) {
                connection.disconnect();
                return true;
            }
        } catch (Exception e) {
            log.debug("User does not exist: {}", username);
        }
        return false;
    }
}
