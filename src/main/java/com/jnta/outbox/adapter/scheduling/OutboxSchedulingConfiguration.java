package com.jnta.outbox.adapter.scheduling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "outbox.processor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxSchedulingConfiguration {
}
