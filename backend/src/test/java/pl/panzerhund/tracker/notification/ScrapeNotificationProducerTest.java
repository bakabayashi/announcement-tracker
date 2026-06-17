package pl.panzerhund.tracker.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.panzerhund.tracker.notification.entity.NotificationType;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ScrapeNotificationProducerTest {

    private NotificationService notificationService;
    private ScrapeNotificationProducer producer;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        producer = new ScrapeNotificationProducer(notificationService);
    }

    @Test
    void newMatchCreatesNewMatchNotification() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        producer.notifyNewMatch(userId, listingId);

        verify(notificationService).create(userId, listingId, NotificationType.NEW_MATCH);
    }

    @Test
    void priceDropCreatesPriceDropNotification() {
        UUID userId = UUID.randomUUID();
        UUID listingId = UUID.randomUUID();

        producer.notifyPriceDrop(userId, listingId);

        verify(notificationService).create(userId, listingId, NotificationType.PRICE_DROP);
    }
}
