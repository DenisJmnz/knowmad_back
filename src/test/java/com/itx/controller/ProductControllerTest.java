package com.itx.controller;

import com.itx.exception.GlobalExceptionHandler;
import com.itx.exception.ProductNotFoundException;
import com.itx.model.ProductDetail;
import com.itx.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("GET /product/{id}/similar - Retorna 200 y lista de productos similares")
    void getSimilarProducts_Success() throws Exception {
        ProductDetail product2 = ProductDetail.builder()
                .id("2")
                .name("Dress")
                .price(19.99)
                .availability(true)
                .build();
        ProductDetail product3 = ProductDetail.builder()
                .id("3")
                .name("Blazer")
                .price(29.99)
                .availability(false)
                .build();

        when(productService.getSimilarProducts("1")).thenReturn(List.of(product2, product3));

        mockMvc.perform(get("/product/1/similar").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("2"))
                .andExpect(jsonPath("$[0].name").value("Dress"))
                .andExpect(jsonPath("$[0].price").value(19.99))
                .andExpect(jsonPath("$[0].availability").value(true))
                .andExpect(jsonPath("$[1].id").value("3"))
                .andExpect(jsonPath("$[1].name").value("Blazer"));
    }

    @Test
    @DisplayName("GET /product/{id}/similar - Retorna 404 y estructura de error exacta cuando el producto no existe")
    void getSimilarProducts_NotFound() throws Exception {
        when(productService.getSimilarProducts("999")).thenThrow(new ProductNotFoundException("999"));

        mockMvc.perform(get("/product/999/similar").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Product not found"))
                .andExpect(jsonPath("$.path").value("/product/999/similar"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
