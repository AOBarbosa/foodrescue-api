package br.com.seudominio.foodrescue.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.services.ProductService;
import br.com.seudominio.foodrescue.domain.dtos.ProductDTO;
import br.com.seudominio.foodrescue.domain.dtos.ProductInventoryUpdateDTO;
import br.com.seudominio.foodrescue.domain.dtos.UpdateProductInventoryRequest;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.security.AuthenticatedPrincipal;
import br.com.seudominio.foodrescue.rest.security.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

class ProductControllerTest {

    private ProductService productService;
    private ProductController productController;
    private AuthenticatedPrincipal principal;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        productController = new ProductController(productService);
        principal = new AuthenticatedPrincipal(1L, UserRole.ESTABLISHMENT);
    }

    @Test
    void updateInventoryUsesAuthenticatedEstablishmentAndReturnsSuccessMessage() {
        UpdateProductInventoryRequest request = new UpdateProductInventoryRequest(5, null);
        ProductInventoryUpdateDTO serviceResult = new ProductInventoryUpdateDTO(mock(ProductDTO.class), false);
        when(productService.updateInventory(10L, 1L, request)).thenReturn(serviceResult);

        ResponseEntity<ApiResponse<ProductInventoryUpdateDTO>> response =
                productController.updateInventory(10L, request, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(serviceResult);
        assertThat(response.getBody().message()).isEqualTo("Inventory updated successfully");
        assertThat(response.getBody().success()).isTrue();
        verify(productService).updateInventory(10L, 1L, request);
    }

    @Test
    void updateInventoryReturnsWarningMessageForPastExpirationDate() {
        UpdateProductInventoryRequest request = new UpdateProductInventoryRequest(null, LocalDate.of(2026, 9, 13));
        ProductInventoryUpdateDTO serviceResult = new ProductInventoryUpdateDTO(mock(ProductDTO.class), true);
        when(productService.updateInventory(10L, 1L, request)).thenReturn(serviceResult);

        ResponseEntity<ApiResponse<ProductInventoryUpdateDTO>> response =
                productController.updateInventory(10L, request, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("Inventory updated; the expiration date is in the past");
        assertThat(response.getBody().data().expirationDateInPast()).isTrue();
    }
}
