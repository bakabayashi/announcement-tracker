package pl.panzerhund.tracker.user.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String name,
        String pictureUrl,
        String role
) {
}
