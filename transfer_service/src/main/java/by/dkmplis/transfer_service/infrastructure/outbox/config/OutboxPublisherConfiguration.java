package by.dkmplis.transfer_service.infrastructure.outbox.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(
        OutboxPublisherProperties.class
)
public class OutboxPublisherConfiguration {
}
