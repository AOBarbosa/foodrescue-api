package br.com.seudominio.perishrescue.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.services.ResourceService;
import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.domain.dtos.ResourceDTO;
import br.com.seudominio.perishrescue.domain.dtos.ResourceInventoryUpdateDTO;
import br.com.seudominio.perishrescue.domain.dtos.UpdateResourceInventoryRequest;
import br.com.seudominio.perishrescue.domain.mappers.ResourceTypeMapper;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;
import br.com.seudominio.perishrescue.rest.dtos.ApiResponse;
import br.com.seudominio.perishrescue.rest.security.AuthenticatedPrincipal;
import br.com.seudominio.perishrescue.rest.security.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Tests the frozen flow of {@link AbstractResourceController} through a
 * minimal concrete subclass, the way an instance would extend it.
 */
class AbstractResourceControllerTest {

    record TestRequest(String name, BigDecimal price) {
    }

    record TestResponse(Long id, String name) {
    }

    static class TestResourceMapper implements ResourceTypeMapper<TestResource, TestRequest, TestResponse> {
        @Override
        public Class<TestResource> resourceType() {
            return TestResource.class;
        }

        @Override
        public TestResource toNewEntity(TestRequest request) {
            return TestResource.builder()
                    .name(request.name())
                    .category("test")
                    .originalPrice(Money.of(request.price()))
                    .build();
        }

        @Override
        public TestResponse toDto(TestResource resource) {
            return new TestResponse(resource.getId(), resource.getName());
        }
    }

    static class TestResourceController
            extends AbstractResourceController<TestResource, TestRequest, TestResponse> {
        TestResourceController(ResourceService resourceService) {
            super(resourceService, new TestResourceMapper());
        }
    }

    private ResourceService resourceService;
    private TestResourceController controller;
    private AuthenticatedPrincipal principal;

    @BeforeEach
    void setUp() {
        resourceService = mock(ResourceService.class);
        controller = new TestResourceController(resourceService);
        principal = new AuthenticatedPrincipal(1L, UserRole.OFFERER);
    }

    private TestResource saved(Long id, String name) {
        TestResource resource = TestResource.builder().name(name).category("test").build();
        resource.setId(id);
        return resource;
    }

    @Test
    void createMapsTheRequestAndRegistersItForTheAuthenticatedOfferer() {
        when(resourceService.register(any(TestResource.class), eq(1L))).thenReturn(saved(10L, "Coxinha"));

        ResponseEntity<ApiResponse<TestResponse>> response =
                controller.create(new TestRequest("Coxinha", new BigDecimal("8.00")), principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(new TestResponse(10L, "Coxinha"));
        assertThat(response.getBody().message()).isEqualTo("Resource registered successfully");
        verify(resourceService).register(any(TestResource.class), eq(1L));
    }

    @Test
    void listAsksTheKernelOnlyForThisControllersResourceType() {
        when(resourceService.findAllForOfferer(1L, TestResource.class))
                .thenReturn(List.of(saved(10L, "Coxinha"), saved(11L, "Pastel")));

        ResponseEntity<ApiResponse<List<TestResponse>>> response = controller.list(principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data())
                .containsExactly(new TestResponse(10L, "Coxinha"), new TestResponse(11L, "Pastel"));
    }

    @Test
    void findByIdLooksUpTheResourceByOwnerAndType() {
        when(resourceService.findResourceOwnedBy(10L, 1L, TestResource.class)).thenReturn(saved(10L, "Coxinha"));

        ResponseEntity<ApiResponse<TestResponse>> response = controller.findById(10L, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isEqualTo(new TestResponse(10L, "Coxinha"));
    }

    @Test
    void updateInventoryUsesAuthenticatedOffererAndReturnsSuccessMessage() {
        UpdateResourceInventoryRequest request = new UpdateResourceInventoryRequest(5, null);
        ResourceInventoryUpdateDTO serviceResult = new ResourceInventoryUpdateDTO(mock(ResourceDTO.class), false);
        when(resourceService.updateInventory(10L, 1L, request)).thenReturn(serviceResult);

        ResponseEntity<ApiResponse<ResourceInventoryUpdateDTO>> response =
                controller.updateInventory(10L, request, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(serviceResult);
        assertThat(response.getBody().message()).isEqualTo("Inventory updated successfully");
        assertThat(response.getBody().success()).isTrue();
        verify(resourceService).findResourceOwnedBy(10L, 1L, TestResource.class);
        verify(resourceService).updateInventory(10L, 1L, request);
    }

    @Test
    void updateInventoryReturnsWarningMessageForPastExpirationDate() {
        UpdateResourceInventoryRequest request = new UpdateResourceInventoryRequest(null, LocalDate.of(2026, 9, 13));
        ResourceInventoryUpdateDTO serviceResult = new ResourceInventoryUpdateDTO(mock(ResourceDTO.class), true);
        when(resourceService.updateInventory(10L, 1L, request)).thenReturn(serviceResult);

        ResponseEntity<ApiResponse<ResourceInventoryUpdateDTO>> response =
                controller.updateInventory(10L, request, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .isEqualTo("Inventory updated; the expiration date is in the past");
        assertThat(response.getBody().data().expirationDateInPast()).isTrue();
    }
}
