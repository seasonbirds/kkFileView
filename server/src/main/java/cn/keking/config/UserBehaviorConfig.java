package cn.keking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 用户行为监控配置类
 * @author kkfileview
 */
@Configuration
@EnableAsync
public class UserBehaviorConfig {

    /**
     * 用户行为监控异步执行器
     */
    @Bean("userBehaviorExecutor")
    public Executor userBehaviorExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("user-behavior-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setDaemon(true);
        executor.initialize();
        return executor;
    }
}
