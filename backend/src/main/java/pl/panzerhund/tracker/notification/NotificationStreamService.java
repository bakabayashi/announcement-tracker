package pl.panzerhund.tracker.notification;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pl.panzerhund.tracker.notification.dto.NotificationResponse;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of per-user SSE connections for the notification bell.
 * Single-instance only; a horizontally scaled deployment would need a shared broker.
 */
@Service
public class NotificationStreamService {

    /** Effectively no server-side timeout; the client reconnects via EventSource if dropped. */
    private static final long TIMEOUT_MS = Long.MAX_VALUE;

    private final ConcurrentHashMap<UUID, Set<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    /** Opens a stream for the user and registers it for future pushes. */
    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        Set<SseEmitter> userEmitters = emittersByUser.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
        userEmitters.add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));
        return emitter;
    }

    /** Pushes a notification to every open stream of the target user. */
    public void publish(UUID userId, NotificationResponse notification) {
        Set<SseEmitter> userEmitters = emittersByUser.get(userId);
        if (userEmitters == null) {
            return;
        }
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (IOException | IllegalStateException e) {
                // Stale connection: complete it, the registered callback removes it.
                emitter.completeWithError(e);
            }
        }
    }

    private void remove(UUID userId, SseEmitter emitter) {
        Set<SseEmitter> userEmitters = emittersByUser.get(userId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emittersByUser.remove(userId, userEmitters);
        }
    }
}
