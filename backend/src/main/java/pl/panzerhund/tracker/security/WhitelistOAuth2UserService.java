package pl.panzerhund.tracker.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import pl.panzerhund.tracker.user.AppUserPrincipal;
import pl.panzerhund.tracker.user.UserRepository;
import pl.panzerhund.tracker.user.entity.User;

import java.util.List;

/**
 * Decorator over {@link DefaultOAuth2UserService}: after loading the Google profile it
 * (1) enforces the email whitelist, (2) creates/updates the User entity.
 * The delegate is injectable (package-private constructor) for testability.
 */
@Service
public class WhitelistOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    static final String ACCESS_DENIED = "access_denied";

    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;
    private final SecurityProperties properties;
    private final UserRepository users;

    @Autowired
    public WhitelistOAuth2UserService(SecurityProperties properties, UserRepository users) {
        this(new DefaultOAuth2UserService(), properties, users);
    }

    WhitelistOAuth2UserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate,
                               SecurityProperties properties,
                               UserRepository users) {
        this.delegate = delegate;
        this.properties = properties;
        this.users = users;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = delegate.loadUser(userRequest);
        enforceWhitelist(user.getAttribute("email"));
        User entity = provision(user);
        return new AppUserPrincipal(
                entity.getId(),
                entity.getEmail(),
                entity.getName(),
                entity.getPictureUrl(),
                user.getAuthorities(),
                user.getAttributes(),
                "sub");
    }

    private void enforceWhitelist(String email) {
        List<String> allowed = properties.getAllowedEmails();
        if (allowed == null || allowed.isEmpty()) {
            return; // dev mode - everyone passes
        }
        boolean permitted = email != null && allowed.stream().anyMatch(email::equalsIgnoreCase);
        if (!permitted) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(ACCESS_DENIED, "Email not in whitelist: " + email, null));
        }
    }

    private User provision(OAuth2User user) {
        String googleSub = user.getName(); // user-name-attribute = "sub"
        User entity = users.findByGoogleSub(googleSub).orElseGet(User::new);
        entity.setGoogleSub(googleSub);
        entity.setEmail(user.getAttribute("email"));
        entity.setName(user.getAttribute("name"));
        entity.setPictureUrl(user.getAttribute("picture"));
        return users.save(entity);
    }
}
