package com.shoplite.inventoryservice;

import jakarta.persistence.*;

@Entity
@Table(name = "stock")
public class Stock {
    @Id
    private Long productId;
    private int quantity;

    public Stock() {}
    public Stock(Long productId, int quantity) { this.productId = productId; this.quantity = quantity; }
    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
