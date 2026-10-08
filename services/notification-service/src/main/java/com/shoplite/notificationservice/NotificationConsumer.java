package com.shoplite.notificationservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

@Component
public class NotificationConsumer {
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    private final ObjectMapper mapper;
    private final Counter sent;
    private final Deque<Map<String, Object>> recent = new ArrayDeque<>();

    public NotificationConsumer(ObjectMapper mapper, MeterRegistry registry) {
        this.mapper = mapper;
        this.sent = Counter.builder("notifications.sent").register(registry);
    }

    @KafkaListener(topics = "order-created", groupId = "notification-service")
    public void onOrder(String message) throws Exception {
        JsonNode n = mapper.readTree(message);
        String userId = n.get("userId").asText();
        long orderId = n.get("orderId").asLong();
        log.info("Confirmation sent userId={} orderId={}", userId, orderId);
        sent.increment();
        synchronized (recent) {
            recent.addFirst(Map.of("userId", userId, "orderId", orderId,
                    "message", "Order #" + orderId + " received. We'll let you know when it ships.",
                    "at", Instant.now().toString()));
            while (recent.size() > 200) recent.removeLast();
        }
    }

    public List<Map<String, Object>> forUser(String userId) {
        synchronized (recent) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (var m : recent) if (userId.equals(m.get("userId")) && out.size() < 20) out.add(m);
            return out;
        }
    }
}
