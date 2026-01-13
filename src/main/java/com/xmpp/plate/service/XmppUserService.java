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
     * Note: This implementation has limitations. For production use with Openfire,
     * consider using the Openfire REST API plugin for proper user management.
     */
    public void deleteXmppUser(String username) {
        // Note: The Smack AccountManager.deleteAccount() only deletes the currently
        // authenticated user. For proper implementation with Openfire, use the REST API
        log.warn("XMPP user deletion for '{}' - consider using Openfire REST API for production", username);
        
        // TODO: Implement proper user deletion via Openfire REST API
        // Example: DELETE http://openfire-server:9090/plugins/restapi/v1/users/{username}
    }

    /**
     * Checks if XMPP user exists
     * Note: This is a placeholder implementation. For production use,
     * query the Openfire REST API to check if a specific user exists.
     */
    public boolean userExists(String username) {
        // TODO: Implement proper user existence check via Openfire REST API
        // Example: GET http://openfire-server:9090/plugins/restapi/v1/users/{username}
        log.debug("User existence check for '{}' - implement REST API call for production", username);
        return false;
    }
}
