package pl.panzerhund.tracker.scraper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import pl.panzerhund.tracker.deduplication.DeduplicationService;
import pl.panzerhund.tracker.scraper.config.ScraperProperties;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Schedules scrape cycles programmatically (no cron annotation): each run starts at a random minute
 * within the nightly window (default 22:00–04:00). After every cycle the next one is re-scheduled for
 * the following window. A cycle scrapes all criteria and then runs deduplication over the freshly-seen
 * listings, as a separate step outside the scrape transaction.
 */
@Service
@RequiredArgsConstructor
public class ScraperSchedulingService {

    private static final Logger log = LoggerFactory.getLogger(ScraperSchedulingService.class);

    private final TaskScheduler taskScheduler;
    private final ScraperService scraperService;
    private final DeduplicationService deduplicationService;
    private final ScraperProperties properties;
    private final Clock clock;

    @PostConstruct
    void scheduleFirstRun() {
        scheduleNext();
    }

    private void scheduleNext() {
        Instant next = nextRun(clock.instant());
        log.info("Next scrape cycle scheduled for {}", next);
        taskScheduler.schedule(this::runCycle, next);
    }

    /** Scrape everything, then deduplicate what the scrape touched; always re-arm the next cycle. */
    void runCycle() {
        Instant runStart = clock.instant();
        log.info("Scrape cycle starting");
        try {
            scraperService.scrapeAll();
            deduplicationService.scanRecentlyActive(runStart);
        } catch (RuntimeException e) {
            log.error("Scrape cycle failed", e);
        } finally {
            scheduleNext();
        }
    }

    /** A random minute offset within the window, measured from the next window start. */
    Instant nextRun(Instant now) {
        ZoneId zone = clock.getZone();
        LocalDateTime base = nextWindowStart(LocalDateTime.ofInstant(now, zone));
        int offsetMinutes = ThreadLocalRandom.current().nextInt(properties.getWindowDurationMinutes());
        return base.plusMinutes(offsetMinutes).atZone(zone).toInstant();
    }

    /** Next occurrence of the window start strictly after {@code now} (so a completed cycle lands in the next window). */
    private LocalDateTime nextWindowStart(LocalDateTime now) {
        LocalTime start = LocalTime.parse(properties.getWindowStart());
        LocalDateTime todayStart = now.toLocalDate().atTime(start);
        return now.isBefore(todayStart) ? todayStart : todayStart.plusDays(1);
    }
}
