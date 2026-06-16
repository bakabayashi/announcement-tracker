package pl.panzerhund.tracker.notification;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pl.panzerhund.tracker.notification.dto.NotificationResponse;
import pl.panzerhund.tracker.notification.entity.NotificationType;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationStreamServiceTest {

    private final NotificationStreamService service = new NotificationStreamService();

    private static NotificationResponse payload() {
        return new NotificationResponse(
                UUID.randomUUID(), null, NotificationType.NEW_MATCH, false, null);
    }

    @Test
    void subscribeReturnsEmitter() {
        SseEmitter emitter = service.subscribe(UUID.randomUUID());
        assertNotNull(emitter);
    }

    @Test
    void publishToUserWithoutSubscribersIsNoOp() {
        assertDoesNotThrow(() -> service.publish(UUID.randomUUID(), payload()));
    }

    @Test
    void publishToSubscribedUserDoesNotThrow() {
        UUID userId = UUID.randomUUID();
        service.subscribe(userId);
        assertDoesNotThrow(() -> service.publish(userId, payload()));
    }
}
