package com.rachit.jobtrackr.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configures ShedLock for distributed scheduler locking.
 *
 * Why ShedLock instead of Quartz?
 * ShedLock is lighter — it adds locking to existing @Scheduled methods
 * without requiring a full job scheduler framework. For the 3 jobs in
 * this app (stale detection, weekly digest, outbox poller), ShedLock
 * is the right level of complexity.
 *
 * Default lock: at most 30 minutes. If a locked job takes longer,
 * the lock expires and another instance can pick it up.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }
}
