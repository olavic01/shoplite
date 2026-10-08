package com.shoplite.productservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repo;
    private final long slowMs;

    public ProductService(ProductRepository repo, @Value("${app.slow-ms:0}") long slowMs) {
        this.repo = repo;
        this.slowMs = slowMs;
    }

    /** Only runs on a cache miss. SLOW_MS lets you simulate a slow database for latency drills. */
    @Cacheable("products")
    public ArrayList<Product> findAll() {
        log.info("Cache miss: loading all products from Postgres");
        pause();
        return new ArrayList<>(repo.findAll());
    }

    @Cacheable(value = "product", key = "#id")
    public Product findOne(Long id) {
        log.info("Cache miss: loading product id={} from Postgres", id);
        pause();
        return repo.findById(id).orElse(null);
    }

    private void pause() {
        if (slowMs > 0) {
            try { Thread.sleep(slowMs); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}
