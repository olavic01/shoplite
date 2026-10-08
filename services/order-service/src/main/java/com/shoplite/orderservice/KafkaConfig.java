package com.shoplite.orderservice;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    public static final String TOPIC = "order-created";

    @Bean
    NewTopic orderCreatedTopic() {
        return TopicBuilder.name(TOPIC).partitions(3).replicas(1).build();
    }
}
