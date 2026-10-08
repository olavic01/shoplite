package com.shoplite.productservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Seeds 8 products on first start. Inventory-service seeds stock for ids 1..8. */
@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final ProductRepository repo;

    public DataSeeder(ProductRepository repo) { this.repo = repo; }

    @Override
    public void run(String... args) {
        if (repo.count() > 0) return;
        repo.saveAll(List.of(
            new Product("Mechanical keyboard", "Tactile switches, hot-swappable, USB-C", new BigDecimal("89.00")),
            new Product("27-inch monitor", "1440p IPS panel, 75 Hz", new BigDecimal("249.00")),
            new Product("USB-C dock", "7 ports, 100 W pass-through charging", new BigDecimal("59.00")),
            new Product("Wireless mouse", "Quiet clicks, 70-day battery", new BigDecimal("29.00")),
            new Product("Laptop stand", "Aluminium, folds flat", new BigDecimal("39.00")),
            new Product("Webcam 1080p", "Auto light correction, privacy shutter", new BigDecimal("49.00")),
            new Product("Noise-cancelling headset", "40 h battery, detachable mic", new BigDecimal("129.00")),
            new Product("Desk lamp", "Dimmable, warm to cool white", new BigDecimal("34.00"))));
        log.info("Seeded {} products", repo.count());
    }
}
