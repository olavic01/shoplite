package com.shoplite.notificationservice;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationConsumer consumer;

    public NotificationController(NotificationConsumer consumer) { this.consumer = consumer; }

    @GetMapping
    public List<Map<String, Object>> mine(@RequestHeader("X-User-Id") String userId) {
        return consumer.forUser(userId);
    }
}
