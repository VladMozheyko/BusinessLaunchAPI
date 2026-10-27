package dev.codelad.lunchservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Конфигурация асинхронных задач.
 * Настраивает пул потоков для @Async методов.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Пул потоков для синхронизации с Food Service.
     * 
     * Параметры:
     * - corePoolSize: 2 — базовое количество потоков (обычно 2 заведения синхронизируются)
     * - maxPoolSize: 5 — максимум при пиковой нагрузке (5 пользователей заходят одновременно)
     * - queueCapacity: 10 — очередь задач, если все потоки заняты
     * - threadNamePrefix: "food-sync-" — для отладки в логах
     * - rejectionPolicy: CallerRunsPolicy — если очередь заполнена, запускается в основном потоке
     */
    @Bean("foodSyncExecutor")
    public Executor foodSyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("food-sync-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        
        log.info("[ASYNC] Food Sync Executor initialized: core=2, max=5, queue=10");
        
        return executor;
    }
}
