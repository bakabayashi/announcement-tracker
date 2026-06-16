package pl.panzerhund.tracker.user.mapper;

import pl.panzerhund.tracker.user.AppUserPrincipal;
import pl.panzerhund.tracker.user.dto.UserResponse;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(AppUserPrincipal principal) {
        return new UserResponse(
                principal.getUserId(),
                principal.getEmail(),
                principal.getFullName(),
                principal.getPictureUrl());
    }
}
