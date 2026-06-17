package pl.panzerhund.tracker.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import pl.panzerhund.tracker.AbstractIntegrationTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MeEndpointTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void returnsCurrentUserFromPrincipal() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUserPrincipal principal = new AppUserPrincipal(
                userId, "user@panzerhund.pl", "User One", "http://pic/1",
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", "sub-1", "email", "user@panzerhund.pl"), "sub");

        mvc.perform(get("/api/v1/me").with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("user@panzerhund.pl"))
                .andExpect(jsonPath("$.name").value("User One"))
                .andExpect(jsonPath("$.pictureUrl").value("http://pic/1"));
    }

    @Test
    void unauthenticatedReturns401() throws Exception {
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }
}
