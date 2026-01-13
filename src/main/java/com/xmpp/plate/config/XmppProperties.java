package com.xmpp.plate.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for XMPP server
 */
@Configuration
@ConfigurationProperties(prefix = "xmpp")
@Data
public class XmppProperties {

    private String host;
    private int port;
    private String domain;
    private String serviceName;
    
    private Admin admin = new Admin();
    private Connection connection = new Connection();

    @Data
    public static class Admin {
        private String username;
        private String password;
    }

    @Data
    public static class Connection {
        private Pool pool = new Pool();
        private int timeout;
        private Reconnect reconnect = new Reconnect();

        @Data
        public static class Pool {
            private int size;
        }

        @Data
        public static class Reconnect {
            private boolean enabled;
            private int delay;
        }
    }
}
