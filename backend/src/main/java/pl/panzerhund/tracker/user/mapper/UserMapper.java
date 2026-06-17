package pl.panzerhund.tracker.user.mapper;

import org.springframework.security.core.GrantedAuthority;
import pl.panzerhund.tracker.user.AppUserPrincipal;
import pl.panzerhund.tracker.user.dto.UserResponse;
import pl.panzerhund.tracker.user.entity.Role;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(AppUserPrincipal principal) {
        return new UserResponse(
                principal.getUserId(),
                principal.getEmail(),
                principal.getFullName(),
                principal.getPictureUrl(),
                resolveRole(principal));
    }

    private static String resolveRole(AppUserPrincipal principal) {
        boolean admin = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(("ROLE_" + Role.ADMIN.name())::equals);
        return admin ? Role.ADMIN.name() : Role.USER.name();
    }
}
