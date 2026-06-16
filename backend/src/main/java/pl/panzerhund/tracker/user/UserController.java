package pl.panzerhund.tracker.user;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.panzerhund.tracker.user.dto.UserResponse;
import pl.panzerhund.tracker.user.mapper.UserMapper;

@RestController
@RequestMapping("/api/v1/me")
public class UserController {

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return UserMapper.toResponse(principal);
    }
}
