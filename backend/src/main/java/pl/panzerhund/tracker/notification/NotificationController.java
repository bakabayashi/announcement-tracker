package pl.panzerhund.tracker.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pl.panzerhund.tracker.notification.dto.NotificationResponse;
import pl.panzerhund.tracker.notification.dto.UnreadCountResponse;
import pl.panzerhund.tracker.notification.mapper.NotificationMapper;
import pl.panzerhund.tracker.user.AppUserPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;
    private final NotificationStreamService streamService;

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return service.listForUser(principal.getUserId()).stream().map(NotificationMapper::toResponse).toList();
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal AppUserPrincipal principal) {
        return new UnreadCountResponse(service.countUnread(principal.getUserId()));
    }

    /** Live push of new notifications (bell badge). Client subscribes via EventSource. */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal AppUserPrincipal principal) {
        return streamService.subscribe(principal.getUserId());
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable UUID id) {
        service.markRead(principal.getUserId(), id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal AppUserPrincipal principal) {
        service.markAllRead(principal.getUserId());
    }
}
