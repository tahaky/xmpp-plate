package com.xmpp.plate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for XMPP Vehicle Messaging Platform
 */
@SpringBootApplication
@EnableAsync
public class XmppPlateApplication {

    public static void main(String[] args) {
        SpringApplication.run(XmppPlateApplication.class, args);
    }
}
