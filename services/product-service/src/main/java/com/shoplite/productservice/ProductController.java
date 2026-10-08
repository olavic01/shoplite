package com.shoplite.productservice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) { this.service = service; }

    @GetMapping
    public List<Product> list() { return service.findAll(); }

    @GetMapping("/{id}")
    public Product get(@PathVariable Long id) {
        Product p = service.findOne(id);
        if (p == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        return p;
    }
}
