package com.itx.service;

import com.itx.exception.ProductNotFoundException;
import com.itx.model.ProductDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ExternalApiResilientClient externalApiClient;
    private final ExecutorService executorService;

    public ProductService(ExternalApiResilientClient externalApiClient) {
        this.externalApiClient = externalApiClient;
        this.executorService = new ThreadPoolExecutor(
                50,
                200,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public List<ProductDetail> getSimilarProducts(String productId) {
        log.info("Getting similar products for productId: {}", productId);
        long startTime = System.currentTimeMillis();

        try {
            List<String> similarIds = externalApiClient.getSimilarProductIds(productId);
            log.debug("Found {} similar product IDs", similarIds.size());

            if (similarIds.isEmpty()) {
                log.warn("No similar products found for productId: {}", productId);
                return List.of();
            }

            List<CompletableFuture<ProductDetail>> futures = similarIds.stream()
                    .map(id -> CompletableFuture.supplyAsync(() -> externalApiClient.getProductDetail(id), executorService))
                    .collect(Collectors.toList());

            List<ProductDetail> similarProducts = futures.stream()
                    .map(future -> {
                        try {
                            return future.join();
                        } catch (Exception e) {
                            log.warn("Failed to get product detail", e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            long duration = System.currentTimeMillis() - startTime;
            log.info("Retrieved {} similar products in {} ms", similarProducts.size(), duration);

            return similarProducts;
        } catch (ProductNotFoundException e) {
            log.warn("Product {} not found: {}", productId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving similar products for productId: {}", productId, e);
            return List.of();
        }
    }
}
