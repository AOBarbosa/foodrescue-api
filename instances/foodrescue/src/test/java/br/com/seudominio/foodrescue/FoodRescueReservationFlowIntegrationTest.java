package br.com.seudominio.foodrescue;

import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.persistence.repositories.BeneficiaryRepository;
import br.com.seudominio.perishrescue.persistence.repositories.OffererRepository;
import br.com.seudominio.perishrescue.rest.security.JwtTokenProvider;
import br.com.seudominio.perishrescue.rest.security.UserRole;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.Filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole rescue flow of the FoodRescue instance through the framework's
 * kernel: register a food item, give it stock, publish an offer, reserve it
 * as a beneficiary, confirm the pickup as the offerer, and hit the
 * instance's daily quota of 3 units per CPF (hot spot 3).
 *
 * <p>The offerer is open all day so {@code PickupWindowRule} never depends on
 * the time the build runs.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:reservationflow;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false"
})
class FoodRescueReservationFlowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private OffererRepository offererRepository;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;
    private String offererToken;
    private String beneficiaryToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();

        Offerer offerer = offererRepository.save(Offerer.builder()
                .name("Padaria 24h")
                .cnpj(String.valueOf(System.nanoTime()))
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.MIN, LocalTime.of(23, 59, 59))
                .email("padaria" + System.nanoTime() + "@example.com")
                .passwordHash("hash")
                .build());
        Beneficiary beneficiary = beneficiaryRepository.save(Beneficiary.builder()
                .name("Maria")
                .document(String.valueOf(System.nanoTime()).substring(0, 11))
                .email("maria" + System.nanoTime() + "@example.com")
                .passwordHash("hash")
                .build());
        offererToken = "Bearer " + jwtTokenProvider.generateToken(offerer.getId(), UserRole.OFFERER);
        beneficiaryToken = "Bearer " + jwtTokenProvider.generateToken(beneficiary.getId(), UserRole.BENEFICIARY);
    }

    private String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    private ResultActions postJson(String url, String token, String json) throws Exception {
        return mockMvc.perform(post(url).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void reservesPicksUpAndThenHitsTheDailyQuotaPerCpf() throws Exception {
        // the offerer registers a food item and puts 10 units in stock
        String item = body(postJson("/food-items", offererToken, """
                {"name": "Pao de queijo", "category": "salgado", "originalPrice": 10.00,
                 "storageTemperature": "AMBIENT", "surpriseBag": false}
                """).andExpect(status().isOk()));
        Integer itemId = JsonPath.read(item, "$.data.id");
        mockMvc.perform(patch("/food-items/" + itemId + "/inventory").header("Authorization", offererToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stockQuantity\": 10}"))
                .andExpect(status().isOk());

        // and publishes it as an offer
        String offer = body(postJson("/offers", offererToken, "{\"resourceId\": " + itemId + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE")));
        Integer offerId = JsonPath.read(offer, "$.data.id");

        // the beneficiary reserves 2 units and gets a pickup code
        String reservation = body(postJson("/reservations", beneficiaryToken,
                "{\"offerId\": " + offerId + ", \"quantity\": 2}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESERVED"))
                .andExpect(jsonPath("$.data.pickupCode").isString()));
        Integer reservationId = JsonPath.read(reservation, "$.data.id");
        String pickupCode = JsonPath.read(reservation, "$.data.pickupCode");

        // the reserved units are held: they leave the sellable stock right away
        mockMvc.perform(get("/food-items/" + itemId).header("Authorization", offererToken))
                .andExpect(jsonPath("$.data.stockQuantity").value(8));

        // the offerer confirms the pickup at the counter (recorded as a sale)
        postJson("/reservations/" + reservationId + "/pickup", offererToken,
                "{\"pickupCode\": \"" + pickupCode + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESCUED"))
                .andExpect(jsonPath("$.data.pickupCode").doesNotExist());
        mockMvc.perform(get("/food-items/" + itemId).header("Authorization", offererToken))
                .andExpect(jsonPath("$.data.stockQuantity").value(8));

        // 2 more units would make 4 today: above FoodRescue's quota of 3 per CPF
        postJson("/reservations", beneficiaryToken, "{\"offerId\": " + offerId + ", \"quantity\": 2}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.data.subErrors[0].code").value("RESCUE_QUOTA_EXCEEDED"));

        // 1 more unit is still within the quota
        postJson("/reservations", beneficiaryToken, "{\"offerId\": " + offerId + ", \"quantity\": 1}")
                .andExpect(status().isOk());
    }

    @Test
    void cancellingGivesTheUnitsBackToTheOffer() throws Exception {
        String item = body(postJson("/food-items", offererToken, """
                {"name": "Sacola surpresa", "category": "sacola", "originalPrice": 20.00,
                 "storageTemperature": "AMBIENT", "surpriseBag": true}
                """).andExpect(status().isOk()));
        Integer itemId = JsonPath.read(item, "$.data.id");
        mockMvc.perform(patch("/food-items/" + itemId + "/inventory").header("Authorization", offererToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"stockQuantity\": 1}"));

        // surprise bags are 75% off (FoodRescue pricing policy, hot spot 2)
        String offer = body(postJson("/offers", offererToken, "{\"resourceId\": " + itemId + "}")
                .andExpect(jsonPath("$.data.discountedPrice").value(5.00)));
        Integer offerId = JsonPath.read(offer, "$.data.id");

        String reservation = body(postJson("/reservations", beneficiaryToken,
                "{\"offerId\": " + offerId + ", \"quantity\": 1}").andExpect(status().isOk()));
        Integer reservationId = JsonPath.read(reservation, "$.data.id");

        mockMvc.perform(get("/offers/" + offerId).header("Authorization", offererToken))
                .andExpect(jsonPath("$.data.status").value("SOLD_OUT"));

        mockMvc.perform(post("/reservations/" + reservationId + "/cancel").header("Authorization", beneficiaryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/offers/" + offerId).header("Authorization", offererToken))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.availableQuantity").value(1));
    }
}
