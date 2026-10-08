package com.shoplite.inventoryservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryConsumer {
    private static final Logger log = LoggerFactory.getLogger(InventoryConsumer.class);

    private final StockRepository repo;
    private final ObjectMapper mapper;
    private final Counter reserved;
    private final Counter rejected;

    public InventoryConsumer(StockRepository repo, ObjectMapper mapper, MeterRegistry registry) {
        this.repo = repo;
        this.mapper = mapper;
        this.reserved = Counter.builder("inventory.reserved").register(registry);
        this.rejected = Counter.builder("inventory.rejected").register(registry);
    }

    @KafkaListener(topics = "order-created", groupId = "inventory-service")
    @Transactional
    public void onOrder(String message) throws Exception {
        JsonNode n = mapper.readTree(message);
        long orderId = n.get("orderId").asLong();
        long productId = n.get("productId").asLong();
        int qty = n.get("quantity").asInt();

        Stock s = repo.findById(productId).orElse(null);
        if (s == null || s.getQuantity() < qty) {
            rejected.increment();
            log.warn("Not enough stock orderId={} productId={} wanted={} have={}",
                    orderId, productId, qty, s == null ? 0 : s.getQuantity());
            return;
        }
        s.setQuantity(s.getQuantity() - qty);
        repo.save(s);
        reserved.increment();
        log.info("Stock reserved orderId={} productId={} qty={} left={}", orderId, productId, qty, s.getQuantity());
    }
}
