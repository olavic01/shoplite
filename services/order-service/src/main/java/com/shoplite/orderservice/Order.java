package com.shoplite.orderservice;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private Long productId;
    private int quantity;
    private String status;
    private Instant createdAt;

    public Order() {}
    public Order(String userId, Long productId, int quantity) {
        this.userId = userId; this.productId = productId; this.quantity = quantity;
        this.status = "PLACED"; this.createdAt = Instant.now();
    }
    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
