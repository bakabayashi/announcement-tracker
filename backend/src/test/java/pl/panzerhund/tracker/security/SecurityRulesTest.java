package pl.panzerhund.tracker.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import pl.panzerhund.tracker.AbstractIntegrationTest;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization rules without a real Google handshake - login is simulated with
 * oauth2Login() from spring-security-test. 404 = "security passed, no handler".
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void publicPathIsPermitted() throws Exception {
        // "/" permitAll -> passes security, no controller -> 404 (not 401/302)
        mvc.perform(get("/")).andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedApiReturns401() throws Exception {
        mvc.perform(get("/api/v1/ping")).andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedBrowserPathRedirectsToGoogle() throws Exception {
        mvc.perform(get("/private"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/google"));
    }

    @Test
    void authenticatedApiPassesSecurity() throws Exception {
        mvc.perform(get("/api/v1/ping").with(oauth2Login()))
                .andExpect(status().isNotFound());
    }

    @Test
    void postWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/api/v1/ping").with(oauth2Login()))
                .andExpect(status().isForbidden());
    }

    @Test
    void postWithCsrfPassesSecurity() throws Exception {
        mvc.perform(post("/api/v1/ping").with(oauth2Login()).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
