package cn.keking.behavior.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 * 用于处理数据库写入和邮件发送等异步任务
 */
@Configuration
public class BehaviorAsyncConfig {

    /**
     * 核心线程数
     */
    private static final int CORE_POOL_SIZE = 2;

    /**
     * 最大线程数
     */
    private static final int MAX_POOL_SIZE = 5;

    /**
     * 队列容量
     */
    private static final int QUEUE_CAPACITY = 100;

    /**
     * 线程名称前缀
     */
    private static final String THREAD_NAME_PREFIX = "Behavior-Async-";

    /**
     * 线程空闲时间（秒）
     */
    private static final int KEEP_ALIVE_SECONDS = 60;

    /**
     * 创建行为监控异步任务执行器
     * 用于异步处理数据库写入和邮件发送
     * @return 线程池任务执行器
     */
    @Bean("behaviorTaskExecutor")
    public Executor behaviorTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
