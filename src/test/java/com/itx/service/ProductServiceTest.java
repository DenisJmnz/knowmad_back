package com.itx.service;

import com.itx.exception.ProductNotFoundException;
import com.itx.model.ProductDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ExternalApiResilientClient externalApiClient;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(externalApiClient);
    }

    @Test
    @DisplayName("Debe devolver productos similares en paralelo correctamente")
    void getSimilarProducts_Success() {
        when(externalApiClient.getSimilarProductIds("1")).thenReturn(List.of("2", "3"));

        ProductDetail p2 = ProductDetail.builder().id("2").name("Dress").price(19.99).availability(true).build();
        ProductDetail p3 = ProductDetail.builder().id("3").name("Blazer").price(29.99).availability(false).build();

        when(externalApiClient.getProductDetail("2")).thenReturn(p2);
        when(externalApiClient.getProductDetail("3")).thenReturn(p3);

        List<ProductDetail> result = productService.getSimilarProducts("1");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("2", result.get(0).getId());
        assertEquals("3", result.get(1).getId());
    }

    @Test
    @DisplayName("Debe degradar elegantemente si un producto similar falla (retorna null)")
    void getSimilarProducts_GracefulDegradation() {
        when(externalApiClient.getSimilarProductIds("1")).thenReturn(List.of("2", "5"));

        ProductDetail p2 = ProductDetail.builder().id("2").name("Dress").price(19.99).availability(true).build();

        when(externalApiClient.getProductDetail("2")).thenReturn(p2);
        when(externalApiClient.getProductDetail("5")).thenReturn(null); // Simulando fallo o 404 del similar

        List<ProductDetail> result = productService.getSimilarProducts("1");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("2", result.get(0).getId());
    }

    @Test
    @DisplayName("Debe propagar ProductNotFoundException si el producto principal no existe")
    void getSimilarProducts_ThrowsProductNotFoundException() {
        when(externalApiClient.getSimilarProductIds("999")).thenThrow(new ProductNotFoundException("999"));

        assertThrows(ProductNotFoundException.class, () -> productService.getSimilarProducts("999"));
    }

    @Test
    @DisplayName("Debe devolver lista vacía si el producto no tiene similares")
    void getSimilarProducts_EmptySimilarIds() {
        when(externalApiClient.getSimilarProductIds("10")).thenReturn(List.of());

        List<ProductDetail> result = productService.getSimilarProducts("10");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
