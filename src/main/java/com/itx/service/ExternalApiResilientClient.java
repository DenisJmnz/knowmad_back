package com.itx.service;

import com.itx.exception.ProductNotFoundException;
import com.itx.model.ProductDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Service
public class ExternalApiResilientClient {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiResilientClient.class);
    private final RestTemplate restTemplate;

    @Value("${external-api.base-url}")
    private String baseUrl;

    @Value("${external-api.max-retries:2}")
    private int maxRetries;

    public ExternalApiResilientClient(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    public List<String> getSimilarProductIds(String productId) {
        log.debug("Fetching similar product IDs for productId: {}", productId);
        String url = baseUrl + "/product/" + productId + "/similarids";

        int attempts = 0;
        while (true) {
            attempts++;
            try {
                String[] ids = restTemplate.getForObject(url, String[].class);
                log.info("Successfully fetched similar product IDs for productId: {}", productId);
                return ids != null ? Arrays.asList(ids) : List.of();
            } catch (HttpClientErrorException.NotFound e) {
                log.warn("Product {} not found when fetching similar product IDs", productId);
                throw new ProductNotFoundException(productId);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    log.warn("Product {} not found when fetching similar product IDs", productId);
                    throw new ProductNotFoundException(productId);
                }
                log.error("Client error fetching similar product IDs for productId: {} (attempt {}/{})",
                        productId, attempts, maxRetries + 1, e);
                return List.of();
            } catch (Exception e) {
                boolean isTimeout = e instanceof ResourceAccessException && e.getCause() instanceof SocketTimeoutException;
                log.error("Error fetching similar product IDs for productId: {} (attempt {}/{}, timeout={})",
                        productId, attempts, maxRetries + 1, isTimeout, e);

                if (attempts > maxRetries || isTimeout) {
                    return List.of();
                }

                try {
                    Thread.sleep(100L * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return List.of();
                }
            }
        }
    }

    public ProductDetail getProductDetail(String productId) {
        log.debug("Fetching product detail for productId: {}", productId);
        String url = baseUrl + "/product/" + productId;

        int attempts = 0;
        while (true) {
            attempts++;
            try {
                ProductDetail product = restTemplate.getForObject(url, ProductDetail.class);
                log.info("Successfully fetched product detail for productId: {}", productId);
                return product;
            } catch (HttpClientErrorException.NotFound e) {
                log.warn("Product detail not found for productId: {}", productId);
                return null;
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    log.warn("Product detail not found for productId: {}", productId);
                    return null;
                }
                log.warn("Client error fetching product detail for productId: {} (attempt {}/{})",
                        productId, attempts, maxRetries + 1);
                return null;
            } catch (Exception e) {
                boolean isTimeout = e instanceof ResourceAccessException && e.getCause() instanceof SocketTimeoutException;
                log.warn("Error fetching product detail for productId: {} (attempt {}/{}, timeout={})",
                        productId, attempts, maxRetries + 1, isTimeout);

                if (attempts > maxRetries || isTimeout) {
                    return null;
                }

                try {
                    Thread.sleep(50L * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
    }
}
