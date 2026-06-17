package pl.panzerhund.tracker.cleanup;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/**
 * Runs {@link CleanupService} daily at 05:00. Scheduled programmatically with a {@link CronTrigger}
 * on the shared {@link TaskScheduler} (no {@code @Scheduled} annotation).
 */
@Component
@RequiredArgsConstructor
public class CleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(CleanupScheduler.class);

    static final String DAILY_AT_5AM = "0 0 5 * * *";

    private final TaskScheduler taskScheduler;
    private final CleanupService cleanupService;

    @PostConstruct
    void schedule() {
        log.info("Scheduling daily cleanup at 05:00 ({})", DAILY_AT_5AM);
        taskScheduler.schedule(cleanupService::runCleanup, new CronTrigger(DAILY_AT_5AM));
    }
}
