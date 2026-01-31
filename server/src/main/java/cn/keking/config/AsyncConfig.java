package cn.keking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置类
 * @author: system
 * @since: 2024/01/01
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 配置用户行为监控异步执行器
     */
    @Bean("userBehaviorExecutor")
    public Executor userBehaviorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(5);
        // 最大线程数
        executor.setMaxPoolSize(10);
        // 队列容量
        executor.setQueueCapacity(100);
        // 线程名前缀
        executor.setThreadNamePrefix("user-behavior-");
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        // 线程空闲时间
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 配置邮件发送异步执行器
     */
    @Bean("mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(2);
        // 最大线程数
        executor.setMaxPoolSize(5);
        // 队列容量
        executor.setQueueCapacity(50);
        // 线程名前缀
        executor.setThreadNamePrefix("mail-sender-");
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        // 线程空闲时间
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }
}