package br.com.seudominio.foodrescue.rest;

import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.persistence.repositories.OffererRepository;
import br.com.seudominio.perishrescue.rest.security.JwtTokenProvider;
import br.com.seudominio.perishrescue.rest.security.UserRole;

import jakarta.servlet.Filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalTime;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives {@code /food-items} through the whole stack: JWT security, the
 * generic request binding and validation inherited from the framework's
 * {@code AbstractResourceController}, the kernel's registration flow and the
 * JOINED mapping of {@code FoodItem} onto {@code resources}/{@code food_items}.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:fooditems;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false"
})
class FoodItemControllerIntegrationTest {

    private static final String VALID_ITEM = """
            {
              "name": "Pao de queijo",
              "category": "salgado",
              "originalPrice": 8.50,
              "allergens": ["LACTOSE", "EGGS"],
              "storageTemperature": "AMBIENT",
              "surpriseBag": false
            }
            """;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private OffererRepository offererRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;
    private String offererToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

        Offerer offerer = offererRepository.save(Offerer.builder()
                .name("Padaria da Esquina")
                .cnpj(String.valueOf(System.nanoTime()))
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(20, 0))
                .email("padaria" + System.nanoTime() + "@example.com")
                .passwordHash("hash")
                .build());
        offererToken = "Bearer " + jwtTokenProvider.generateToken(offerer.getId(), UserRole.OFFERER);
    }

    @Test
    void registersAFoodItemAndListsItBackWithItsFoodAttributes() throws Exception {
        mockMvc.perform(post("/food-items")
                        .header("Authorization", offererToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ITEM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("Pao de queijo"))
                .andExpect(jsonPath("$.data.currentPrice").value(8.50))
                .andExpect(jsonPath("$.data.allergens", containsInAnyOrder("LACTOSE", "EGGS")))
                .andExpect(jsonPath("$.data.storageTemperature").value("AMBIENT"));

        mockMvc.perform(get("/food-items").header("Authorization", offererToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Pao de queijo"));
    }

    @Test
    void rejectsAnInvalidRequestUsingTheInstanceDtoConstraints() throws Exception {
        mockMvc.perform(post("/food-items")
                        .header("Authorization", offererToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Pao de queijo", "category": "salgado", "originalPrice": 8.50 }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refusesARequestWithoutAToken() throws Exception {
        mockMvc.perform(post("/food-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ITEM))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refusesABeneficiary() throws Exception {
        String beneficiaryToken = "Bearer " + jwtTokenProvider.generateToken(999L, UserRole.BENEFICIARY);

        mockMvc.perform(post("/food-items")
                        .header("Authorization", beneficiaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ITEM))
                .andExpect(status().isForbidden());
    }
}
