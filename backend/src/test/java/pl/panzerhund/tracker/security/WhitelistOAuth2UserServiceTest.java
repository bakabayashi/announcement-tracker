package pl.panzerhund.tracker.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import pl.panzerhund.tracker.user.AppUserPrincipal;
import pl.panzerhund.tracker.user.UserRepository;
import pl.panzerhund.tracker.user.entity.Role;
import pl.panzerhund.tracker.user.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class WhitelistOAuth2UserServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private SecurityProperties properties;
    private UserRepository users;
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;
    private WhitelistOAuth2UserService service;

    @BeforeEach
    void setUp() {
        properties = new SecurityProperties();
        users = mock(UserRepository.class);
        delegate = mock(OAuth2UserService.class);
        service = new WhitelistOAuth2UserService(delegate, properties, users);
        when(users.findByGoogleSub(any())).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(USER_ID); // simulate generated id
            return saved;
        });
    }

    private void googleReturns(String email) {
        OAuth2User user = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", "sub-123", "email", email, "name", "Jan Kowalski", "picture", "http://pic/1"),
                "sub");
        when(delegate.loadUser(any())).thenReturn(user);
    }

    @Test
    void emptyWhitelistAllowsEveryoneAndProvisions() {
        properties.setAllowedEmails(List.of());
        googleReturns("anyone@example.com");

        OAuth2User result = service.loadUser(null);

        assertThat(result).isNotNull();
        verify(users).save(any(User.class));
    }

    @Test
    void whitelistedEmailAllowedCaseInsensitive() {
        properties.setAllowedEmails(List.of("allowed@panzerhund.pl"));
        googleReturns("Allowed@Panzerhund.PL");

        assertThatNoException().isThrownBy(() -> service.loadUser(null));
        verify(users).save(any(User.class));
    }

    @Test
    void nonWhitelistedEmailRejectedAndNotProvisioned() {
        properties.setAllowedEmails(List.of("allowed@panzerhund.pl"));
        googleReturns("intruder@example.com");

        assertThatThrownBy(() -> service.loadUser(null))
                .isInstanceOf(OAuth2AuthenticationException.class);
        verify(users, never()).save(any());
    }

    @Test
    void provisionsUserWithGoogleAttributes() {
        properties.setAllowedEmails(List.of());
        googleReturns("new@example.com");

        service.loadUser(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getGoogleSub()).isEqualTo("sub-123");
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getName()).isEqualTo("Jan Kowalski");
        assertThat(saved.getPictureUrl()).isEqualTo("http://pic/1");
    }

    @Test
    void returnsEnrichedPrincipalWithInternalUserId() {
        properties.setAllowedEmails(List.of());
        googleReturns("new@example.com");

        OAuth2User result = service.loadUser(null);

        assertThat(result).isInstanceOf(AppUserPrincipal.class);
        AppUserPrincipal principal = (AppUserPrincipal) result;
        assertThat(principal.getUserId()).isEqualTo(USER_ID);
        assertThat(principal.getEmail()).isEqualTo("new@example.com");
        assertThat(principal.getFullName()).isEqualTo("Jan Kowalski");
        assertThat(principal.getName()).isEqualTo("sub-123"); // OAuth2 name attribute = sub
    }

    @Test
    void adminEmailGetsAdminRoleAndAuthority() {
        properties.setAdminEmails(List.of("Boss@Panzerhund.PL"));
        googleReturns("boss@panzerhund.pl");

        OAuth2User result = service.loadUser(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void nonAdminEmailGetsUserRole() {
        properties.setAdminEmails(List.of("boss@panzerhund.pl"));
        googleReturns("regular@example.com");

        OAuth2User result = service.loadUser(null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void roleRefreshedToUserWhenRemovedFromAdminList() {
        // existing admin user logs in while no longer on the admin list -> demoted to USER
        User existing = new User();
        existing.setRole(Role.ADMIN);
        when(users.findByGoogleSub(any())).thenReturn(Optional.of(existing));
        properties.setAdminEmails(List.of());
        googleReturns("former-admin@example.com");

        service.loadUser(null);

        assertThat(existing.getRole()).isEqualTo(Role.USER);
    }
}
