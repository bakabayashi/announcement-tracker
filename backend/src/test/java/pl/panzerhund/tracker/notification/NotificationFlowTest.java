package pl.panzerhund.tracker.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import pl.panzerhund.tracker.AbstractIntegrationTest;
import pl.panzerhund.tracker.category.entity.Category;
import pl.panzerhund.tracker.listing.ListingRepository;
import pl.panzerhund.tracker.listing.entity.Listing;
import pl.panzerhund.tracker.listing.entity.ListingStatus;
import pl.panzerhund.tracker.listing.entity.Source;
import pl.panzerhund.tracker.notification.entity.Notification;
import pl.panzerhund.tracker.notification.entity.NotificationType;
import pl.panzerhund.tracker.user.AppUserPrincipal;
import pl.panzerhund.tracker.user.UserRepository;
import pl.panzerhund.tracker.user.entity.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationFlowTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private NotificationRepository notifications;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ListingRepository listings;

    @Autowired
    private UserRepository users;

    private AppUserPrincipal principal;
    private Listing listing;

    @BeforeEach
    void setUp() {
        notifications.deleteAll();
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

    private Notification seed(NotificationType type) {
        Notification n = new Notification();
        n.setUser(users.findById(principal.getUserId()).orElseThrow());
        n.setListing(listing);
        n.setType(type);
        return notifications.save(n);
    }

    @Test
    void listCountReadAndReadAll() throws Exception {
        Notification first = seed(NotificationType.NEW_MATCH);
        seed(NotificationType.PRICE_DROP);

        // list (newest first)
        mvc.perform(get("/api/v1/notifications").with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].listing.id").value(listing.getId().toString()));

        // unread count
        mvc.perform(get("/api/v1/notifications/unread-count").with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        // mark one read
        mvc.perform(post("/api/v1/notifications/{id}/read", first.getId())
                        .with(oauth2Login().oauth2User(principal)).with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/notifications/unread-count").with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        // mark all read
        mvc.perform(post("/api/v1/notifications/read-all")
                        .with(oauth2Login().oauth2User(principal)).with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/notifications/unread-count").with(oauth2Login().oauth2User(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void markingUnknownNotificationReturns404() throws Exception {
        mvc.perform(post("/api/v1/notifications/{id}/read", UUID.randomUUID())
                        .with(oauth2Login().oauth2User(principal)).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createPersistsAndExposesNotification() {
        Notification created = notificationService.create(
                principal.getUserId(), listing.getId(), NotificationType.REPOSTED);

        Notification reloaded = notifications.findById(created.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(NotificationType.REPOSTED, reloaded.getType());
        org.junit.jupiter.api.Assertions.assertFalse(reloaded.isRead());
    }
}
