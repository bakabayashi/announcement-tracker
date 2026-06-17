package pl.panzerhund.tracker.scraper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.scheduling.TaskScheduler;
import pl.panzerhund.tracker.deduplication.DeduplicationService;
import pl.panzerhund.tracker.scraper.config.ScraperProperties;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScraperSchedulingServiceTest {

    private static final ZoneId ZONE = ZoneId.of("UTC");

    private TaskScheduler taskScheduler;
    private ScraperService scraperService;
    private DeduplicationService deduplicationService;
    private ScraperProperties properties;

    @BeforeEach
    void setUp() {
        taskScheduler = mock(TaskScheduler.class);
        scraperService = mock(ScraperService.class);
        deduplicationService = mock(DeduplicationService.class);
        properties = new ScraperProperties(); // window 22:00, duration 360 by default
    }

    private ScraperSchedulingService serviceAt(LocalDateTime now) {
        Clock clock = Clock.fixed(at(now), ZONE);
        return new ScraperSchedulingService(taskScheduler, scraperService, deduplicationService, properties, clock);
    }

    private Instant at(LocalDateTime ldt) {
        return ldt.atZone(ZONE).toInstant();
    }

    @Test
    void nextRunFallsWithinTonightsWindowWhenBeforeIt() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 17, 14, 0);
        ScraperSchedulingService service = serviceAt(now);
        Instant windowStart = at(LocalDateTime.of(2026, 6, 17, 22, 0));
        Instant windowEnd = at(LocalDateTime.of(2026, 6, 18, 4, 0));

        for (int i = 0; i < 50; i++) {
            assertThat(service.nextRun(at(now))).isBetween(windowStart, windowEnd);
        }
    }

    @Test
    void nextRunUsesTheFollowingNightWhenAlreadyInsideTheWindow() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 17, 23, 0); // inside 22:00–04:00
        ScraperSchedulingService service = serviceAt(now);
        Instant windowStart = at(LocalDateTime.of(2026, 6, 18, 22, 0));
        Instant windowEnd = at(LocalDateTime.of(2026, 6, 19, 4, 0));

        assertThat(service.nextRun(at(now))).isBetween(windowStart, windowEnd);
    }

    @Test
    void runCycleScrapesThenDeduplicatesThenReschedules() {
        ScraperSchedulingService service = serviceAt(LocalDateTime.of(2026, 6, 17, 14, 0));

        service.runCycle();

        InOrder order = inOrder(scraperService, deduplicationService, taskScheduler);
        order.verify(scraperService).scrapeAll();
        order.verify(deduplicationService).scanRecentlyActive(any());
        order.verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    void runCycleReschedulesEvenWhenScrapeFails() {
        when(scraperService.scrapeAll()).thenThrow(new RuntimeException("boom"));
        ScraperSchedulingService service = serviceAt(LocalDateTime.of(2026, 6, 17, 14, 0));

        service.runCycle();

        verify(deduplicationService, never()).scanRecentlyActive(any());
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }
}
