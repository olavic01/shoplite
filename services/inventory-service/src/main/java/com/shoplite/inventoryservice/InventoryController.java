package com.shoplite.inventoryservice;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
    private final StockRepository repo;

    public InventoryController(StockRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Stock> all() { return repo.findAll(); }
}

/** Seeds 100 units for product ids 1..8 (matches product-service seed data). */
@Component
class StockSeeder implements CommandLineRunner {
    private final StockRepository repo;

    StockSeeder(StockRepository repo) { this.repo = repo; }

    @Override
    public void run(String... args) {
        for (long id = 1; id <= 8; id++) {
            if (!repo.existsById(id)) repo.save(new Stock(id, 100));
        }
    }
}
