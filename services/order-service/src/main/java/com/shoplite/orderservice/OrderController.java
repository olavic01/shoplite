package com.shoplite.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    public record NewOrder(Long productId, int quantity) {}

    private final OrderRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final Counter created;

    public OrderController(OrderRepository repo, KafkaTemplate<String, String> kafka,
                           ObjectMapper mapper, MeterRegistry registry) {
        this.repo = repo;
        this.kafka = kafka;
        this.mapper = mapper;
        this.created = Counter.builder("orders.created").description("Orders placed").register(registry);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order place(@RequestHeader("X-User-Id") String userId, @RequestBody NewOrder req) {
        if (req.productId() == null || req.quantity() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId and quantity >= 1 required");
        }
        Order o = repo.save(new Order(userId, req.productId(), req.quantity()));
        try {
            String json = mapper.writeValueAsString(Map.of(
                    "orderId", o.getId(), "userId", userId,
                    "productId", o.getProductId(), "quantity", o.getQuantity()));
            kafka.send(KafkaConfig.TOPIC, String.valueOf(o.getId()), json);
        } catch (Exception e) {
            log.error("Could not publish OrderCreated for order {}", o.getId(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Order saved but event not sent");
        }
        created.increment();
        log.info("Order placed orderId={} userId={} productId={} qty={}", o.getId(), userId, o.getProductId(), o.getQuantity());
        return o;
    }

    @GetMapping
    public List<Order> mine(@RequestHeader("X-User-Id") String userId) {
        return repo.findByUserIdOrderByIdDesc(userId);
    }
}
