package pl.panzerhund.tracker.listing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import pl.panzerhund.tracker.AbstractIntegrationTest;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.dto.SaveListingRequest;
import pl.panzerhund.tracker.listing.dto.UpdateSavedListingRequest;
import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.user.AppUserPrincipal;
import pl.panzerhund.tracker.user.UserRepository;
import pl.panzerhund.tracker.user.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SavedListingFlowTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ListingRepository listings;

    @Autowired
    private SavedListingRepository savedListings;

    @Autowired
    private PriceHistoryRepository priceHistory;

    @Autowired
    private UserRepository users;

    @Autowired
    private ObjectMapper objectMapper;

    private AppUserPrincipal principal;
    private Listing listing;

    @BeforeEach
    void setUp() {
        savedListings.deleteAll();
        priceHistory.deleteAll();
        listings.deleteAll();
        users.deleteAll();

        User user = new User();
        user.setGoogleSub("sub-1");
        user.setEmail("user@panzerhund.pl");
        user.setName("User One");
        user = users.save(user);

        principal = new AppUserPrincipal(
                user.getId(), user.getEmail(), user.getName(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", "sub-1", "email", user.getEmail()), "sub");

        Listing l = new Listing();
        l.setSource(Source.OLX);
        l.setExternalId("a");
        l.setCategory(Category.PLOT);
        l.setTitle("Plot A");
        l.setUrl("https://olx.pl/a");
        l.setStatus(ListingStatus.ACTIVE);
        l.setPrice(new BigDecimal("100000"));
        listing = listings.save(l);
    }

    @Test
    void saveListReadUpdateDelete() throws Exception {
        // save
        String saveBody = objectMapper.writeValueAsString(new SaveListingRequest(listing.getId(), "note1"));
        mvc.perform(post("/api/v1/saved-listings")
                        .with(oauth2Login().oauth2User(principal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(saveBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notes").value("note1"))
                .andExpect(jsonPath("$.listing.id").value(listing.getId().toString()));

        // list
        mvc.perform(get("/api/v1/saved-listings").with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        var savedId = savedListings.findByUser_Id(principal.getUserId()).get(0).getId();

        // update notes
        String updateBody = objectMapper.writeValueAsString(new UpdateSavedListingRequest("note2"));
        mvc.perform(patch("/api/v1/saved-listings/{id}", savedId)
                        .with(oauth2Login().oauth2User(principal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("note2"));

        // delete
        mvc.perform(delete("/api/v1/saved-listings/{id}", savedId)
                        .with(oauth2Login().oauth2User(principal)).with(csrf()))
                .andExpect(status().isNoContent());

        // empty again
        mvc.perform(get("/api/v1/saved-listings").with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void savingUnknownListingReturns404() throws Exception {
        String body = objectMapper.writeValueAsString(
                new SaveListingRequest(java.util.UUID.randomUUID(), null));
        mvc.perform(post("/api/v1/saved-listings")
                        .with(oauth2Login().oauth2User(principal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }
}
