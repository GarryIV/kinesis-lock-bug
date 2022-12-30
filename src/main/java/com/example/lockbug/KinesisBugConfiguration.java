package com.example.lockbug;

import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("kinesis")
@Configuration
public class KinesisBugConfiguration {
    private static final Log logger = LogFactory.getLog(KinesisBugConfiguration.class);

    @Bean
    public Consumer<String> myConsumer() {
        return s -> logger.info("GOT MESSAGE: " + s);
    }

    @Bean
    public ApplicationPidFileWriter doSomethingAfterStartup() {
        var pidFileWriter = new ApplicationPidFileWriter();
        pidFileWriter.setTriggerEventType(ApplicationReadyEvent.class);
        return pidFileWriter;
    }
}
