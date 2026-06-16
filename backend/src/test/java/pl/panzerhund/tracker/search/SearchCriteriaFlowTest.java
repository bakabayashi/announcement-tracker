package pl.panzerhund.tracker.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.search.dto.SearchCriteriaRequest;
import pl.panzerhund.tracker.user.AppUserPrincipal;
import pl.panzerhund.tracker.user.UserRepository;
import pl.panzerhund.tracker.user.entity.User;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SearchCriteriaFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SearchCriteriaRepository searchCriteria;

    @Autowired
    private UserRepository users;

    @Autowired
    private ObjectMapper objectMapper;

    private AppUserPrincipal principal;

    @BeforeEach
    void setUp() {
        searchCriteria.deleteAll();
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
    }

    @Test
    void createListUpdateDelete() throws Exception {
        // create
        String createBody = objectMapper.writeValueAsString(new SearchCriteriaRequest(
                "Plots near Krakow", Category.PLOT, Map.of("region", "malopolskie", "priceMax", 200000)));
        mvc.perform(post("/api/v1/search-criteria")
                        .with(oauth2Login().oauth2User(principal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Plots near Krakow"))
                .andExpect(jsonPath("$.category").value("PLOT"))
                .andExpect(jsonPath("$.filters.region").value("malopolskie"));

        // list
        mvc.perform(get("/api/v1/search-criteria").with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        UUID id = searchCriteria.findByUser_Id(principal.getUserId()).get(0).getId();

        // update
        String updateBody = objectMapper.writeValueAsString(new SearchCriteriaRequest(
                "Cars under 50k", Category.CAR, Map.of("priceMax", 50000)));
        mvc.perform(put("/api/v1/search-criteria/{id}", id)
                        .with(oauth2Login().oauth2User(principal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cars under 50k"))
                .andExpect(jsonPath("$.category").value("CAR"));

        // delete
        mvc.perform(delete("/api/v1/search-criteria/{id}", id)
                        .with(oauth2Login().oauth2User(principal)).with(csrf()))
                .andExpect(status().isNoContent());

        // empty again
        mvc.perform(get("/api/v1/search-criteria").with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void updatingUnknownCriteriaReturns404() throws Exception {
        String body = objectMapper.writeValueAsString(new SearchCriteriaRequest(
                "X", Category.PLOT, Map.of()));
        mvc.perform(put("/api/v1/search-criteria/{id}", UUID.randomUUID())
                        .with(oauth2Login().oauth2User(principal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidRequestReturns400() throws Exception {
        // blank name
        String body = objectMapper.writeValueAsString(new SearchCriteriaRequest(
                "", Category.PLOT, Map.of()));
        mvc.perform(post("/api/v1/search-criteria")
                        .with(oauth2Login().oauth2User(principal)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
