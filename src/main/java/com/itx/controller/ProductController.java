package com.itx.controller;

import com.itx.model.ProductDetail;
import com.itx.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping(value = "/{productId}/similar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProductDetail>> getSimilarProducts(@PathVariable String productId) {
        log.info("Received request for similar products of productId: {}", productId);
        List<ProductDetail> similarProducts = productService.getSimilarProducts(productId);
        return ResponseEntity.ok(similarProducts);
    }
}
