package pl.panzerhund.tracker.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/** Login whitelist configuration (app.security.allowed-emails). */
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityProperties {

    /** Allowed email addresses. Empty = dev mode (every authenticated Google user passes). */
    private List<String> allowedEmails = new ArrayList<>();
}
