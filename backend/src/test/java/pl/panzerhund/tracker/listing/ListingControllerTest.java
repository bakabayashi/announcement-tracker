package pl.panzerhund.tracker.listing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import pl.panzerhund.tracker.AbstractIntegrationTest;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.Source;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ListingControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ListingRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private Listing save(String externalId, Category category, String title, BigDecimal price, String region) {
        Listing l = new Listing();
        l.setSource(Source.OLX);
        l.setExternalId(externalId);
        l.setCategory(category);
        l.setTitle(title);
        l.setUrl("https://olx.pl/" + externalId);
        l.setStatus(ListingStatus.ACTIVE);
        l.setPrice(price);
        l.setRegion(region);
        return repository.save(l);
    }

    @Test
    void listReturnsPagedListings() throws Exception {
        save("a", Category.PLOT, "Plot A", new BigDecimal("100000"), "mazowieckie");
        save("b", Category.CAR, "Car B", new BigDecimal("50000"), "slaskie");

        mvc.perform(get("/api/v1/listings").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void listFiltersByCategory() throws Exception {
        save("a", Category.PLOT, "Plot A", new BigDecimal("100000"), "mazowieckie");
        save("b", Category.CAR, "Car B", new BigDecimal("50000"), "slaskie");

        mvc.perform(get("/api/v1/listings").param("category", "PLOT").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].category").value("PLOT"));
    }

    @Test
    void detailReturnsListing() throws Exception {
        Listing saved = save("a", Category.PLOT, "Plot A", new BigDecimal("100000"), "mazowieckie");

        mvc.perform(get("/api/v1/listings/{id}", saved.getId()).with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.title").value("Plot A"));
    }

    @Test
    void detailReturns404ForUnknownId() throws Exception {
        mvc.perform(get("/api/v1/listings/{id}", UUID.randomUUID()).with(oauth2Login()))
                .andExpect(status().isNotFound());
    }
}
