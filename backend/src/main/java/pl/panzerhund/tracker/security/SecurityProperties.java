package pl.panzerhund.tracker.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/** Login whitelist and role configuration (app.security.*). */
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityProperties {

    /** Allowed email addresses. Empty = dev mode (every authenticated Google user passes). */
    private List<String> allowedEmails = new ArrayList<>();

    /** Email addresses granted the ADMIN role on login; everyone else is USER. */
    private List<String> adminEmails = new ArrayList<>();
}
