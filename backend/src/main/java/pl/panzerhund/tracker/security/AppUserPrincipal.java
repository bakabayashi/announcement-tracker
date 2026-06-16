package pl.panzerhund.tracker.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Authenticated principal enriched with our domain User snapshot (taken at login).
 * {@link #getName()} still returns the OAuth2 'sub'; use {@link #getFullName()} for the display name.
 */
@Getter
public class AppUserPrincipal extends DefaultOAuth2User {

    private final UUID userId;
    private final String email;
    private final String fullName;
    private final String pictureUrl;

    public AppUserPrincipal(UUID userId,
                            String email,
                            String fullName,
                            String pictureUrl,
                            Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey) {
        super(authorities, attributes, nameAttributeKey);
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.pictureUrl = pictureUrl;
    }
}
