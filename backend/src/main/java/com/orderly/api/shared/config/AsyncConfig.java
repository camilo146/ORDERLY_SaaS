package com.orderly.api.shared.config;

import com.orderly.api.shared.tenant.TenantContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Async executor that propagates TenantContext into @Async threads.
 * ALL @Async methods in the system must use "asyncExecutor".
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("orderly-async-");
        executor.setTaskDecorator(tenantPropagatingDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * Captures TenantContext from the submitting thread and restores it
     * in the worker thread — critical for multi-tenant @Async methods.
     */
    private TaskDecorator tenantPropagatingDecorator() {
        return runnable -> {
            UUID tenantId = TenantContext.getTenantId().orElse(null);
            return () -> {
                try {
                    if (tenantId != null) {
                        TenantContext.setTenantId(tenantId);
                    }
                    runnable.run();
                } finally {
                    TenantContext.clear();
                }
            };
        };
    }
}
