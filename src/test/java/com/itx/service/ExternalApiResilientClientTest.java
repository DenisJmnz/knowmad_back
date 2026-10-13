package com.itx.service;

import com.itx.exception.ProductNotFoundException;
import com.itx.model.ProductDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ExternalApiResilientClientTest {

    private ExternalApiResilientClient client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        client = new ExternalApiResilientClient(builder);
        ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:3001");
        ReflectionTestUtils.setField(client, "maxRetries", 1);

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(client, "restTemplate");
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    @DisplayName("getSimilarProductIds - Éxito retorna lista de IDs")
    void getSimilarProductIds_Success() {
        mockServer.expect(requestTo("http://localhost:3001/product/1/similarids"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[\"2\", \"3\", \"4\"]", MediaType.APPLICATION_JSON));

        List<String> ids = client.getSimilarProductIds("1");

        assertNotNull(ids);
        assertEquals(3, ids.size());
        assertEquals("2", ids.get(0));
        mockServer.verify();
    }

    @Test
    @DisplayName("getSimilarProductIds - 404 lanza ProductNotFoundException")
    void getSimilarProductIds_ThrowsProductNotFoundException() {
        mockServer.expect(requestTo("http://localhost:3001/product/999/similarids"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(ProductNotFoundException.class, () -> client.getSimilarProductIds("999"));
        mockServer.verify();
    }

    @Test
    @DisplayName("getProductDetail - Éxito retorna ProductDetail")
    void getProductDetail_Success() {
        mockServer.expect(requestTo("http://localhost:3001/product/2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":\"2\",\"name\":\"Dress\",\"price\":19.99,\"availability\":true}", MediaType.APPLICATION_JSON));

        ProductDetail detail = client.getProductDetail("2");

        assertNotNull(detail);
        assertEquals("2", detail.getId());
        assertEquals("Dress", detail.getName());
        assertEquals(19.99, detail.getPrice());
        assertEquals(true, detail.getAvailability());
        mockServer.verify();
    }

    @Test
    @DisplayName("getProductDetail - 404 retorna null para degradación elegante")
    void getProductDetail_ReturnsNullOnNotFound() {
        mockServer.expect(requestTo("http://localhost:3001/product/5"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ProductDetail detail = client.getProductDetail("5");

        assertNull(detail);
        mockServer.verify();
    }
}
