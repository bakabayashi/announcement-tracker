package pl.panzerhund.tracker.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Clock;

/**
 * Shared scheduling infrastructure. We schedule programmatically on a {@link ThreadPoolTaskScheduler}
 * (no {@code @Scheduled} annotations) because the scrape window needs a randomized start each night.
 */
@Configuration
public class SchedulingConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2); // scrape cycle + nightly cleanup
        scheduler.setThreadNamePrefix("tracker-sched-");
        scheduler.initialize();
        return scheduler;
    }

    /** Injected wherever "now" is needed, so scheduling and TTL logic stay testable with a fixed clock. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
