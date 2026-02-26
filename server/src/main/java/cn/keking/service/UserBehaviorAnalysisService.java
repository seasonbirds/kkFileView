package cn.keking.service;

import cn.keking.config.ConfigConstants;
import cn.keking.model.UserBehavior;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 用户行为分析核心服务
 * 
 * <p>作为用户行为监控的核心组件，提供以下功能：</p>
 * <ol>
 *   <li><strong>行为记录</strong>：异步保存用户访问记录到数据库</li>
 *   <li><strong>限流检查</strong>：使用本地缓存高效执行滑动窗口限流和每日访问限制检查</li>
 *   <li><strong>告警去重</strong>：对同一IP的限流告警只发送一次，避免邮件轰炸</li>
 *   <li><strong>每日重置</strong>：每天午夜自动重置每日访问计数和告警状态</li>
 * </ol>
 * 
 * <p><strong>限流策略：</strong></p>
 * <ul>
 *   <li>周期限流（滑动窗口）：X分钟内最多访问Y次，超限返回"请求太频繁"</li>
 *   <li>每日限流：当天最多访问Z次，超限返回"用户行为异常"</li>
 * </ul>
 * 
 * <p><strong>设计亮点：</strong></p>
 * <ul>
 *   <li>限流检查使用本地缓存，避免数据库查询开销</li>
 *   <li>数据库写入和邮件发送均异步执行，不阻塞请求处理</li>
 *   <li>使用Set实现告警去重，避免重复发送</li>
 * </ul>
 * 
 * @author kkFileView Team
 */
public class UserBehaviorAnalysisService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserBehaviorAnalysisService.class);

    private final AlertEmailService alertEmailService;
    private final UserBehaviorCache userBehaviorCache;
    private final AsyncDatabaseService asyncDatabaseService;
    
    /**
     * 周期限流告警已发送集合（去重）
     * 使用ConcurrentHashMap.newKeySet()实现线程安全的Set
     */
    private final Set<String> periodAlertSentSet = ConcurrentHashMap.newKeySet();
    
    /**
     * 每日限流告警已发送集合（去重）
     */
    private final Set<String> dailyAlertSentSet = ConcurrentHashMap.newKeySet();
    
    /**
     * 每日重置任务调度器
     * 守护线程，在JVM退出时自动关闭
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "UserBehaviorDailyReset");
        t.setDaemon(true);
        return t;
    });

    /**
     * 构造函数
     * 
     * @param alertEmailService      告警邮件服务
     * @param userBehaviorCache      本地限流缓存服务
     * @param asyncDatabaseService   异步数据库写入服务
     */
    public UserBehaviorAnalysisService(AlertEmailService alertEmailService,
                                        UserBehaviorCache userBehaviorCache,
                                        AsyncDatabaseService asyncDatabaseService) {
        this.alertEmailService = alertEmailService;
        this.userBehaviorCache = userBehaviorCache;
        this.asyncDatabaseService = asyncDatabaseService;
        startDailyResetTask();
    }

    /**
     * 记录用户行为
     * 
     * <p>执行流程：</p>
     * <ol>
     *   <li>创建UserBehavior实体对象</li>
     *   <li>异步保存到数据库（不阻塞）</li>
     *   <li>同步更新本地缓存计数（低延迟）</li>
     * </ol>
     * 
     * @param ipAddress   客户端IP地址
     * @param fileName    访问的文件名
     * @param requestUrl  完整的请求URL
     */
    public void recordBehavior(String ipAddress, String fileName, String requestUrl) {
        UserBehavior behavior = new UserBehavior(ipAddress, fileName, requestUrl);
        asyncDatabaseService.saveRecordAsync(behavior);
        int periodMinutes = ConfigConstants.getRateLimitPeriodMinutes();
        userBehaviorCache.recordAccess(ipAddress, periodMinutes);
    }

    /**
     * 检查限流状态
     * 
     * <p>检查顺序：</p>
     * <ol>
     *   <li>首先检查每日访问次数是否超限（最严重的限制）</li>
     *   <li>然后检查周期内访问次数是否超限</li>
     *   <li>都未超限时返回允许</li>
     * </ol>
     * 
     * <p><strong>告警策略：</strong>每个IP的每种类型限流告警只发送一次</p>
     * <p><strong>告警恢复：</strong>当周期内请求数降到阈值的50%以下时清除告警标记，允许下次触发时再次告警</p>
     * 
     * @param ipAddress  客户端IP地址
     * @return           限流检查结果
     */
    public RateLimitResult checkRateLimit(String ipAddress) {
        int periodMinutes = ConfigConstants.getRateLimitPeriodMinutes();
        int maxRequests = ConfigConstants.getRateLimitMaxRequests();
        int dailyMaxRequests = ConfigConstants.getDailyRateLimitMaxRequests();

        int periodCount = userBehaviorCache.getPeriodCount(ipAddress, periodMinutes);
        int dailyCount = userBehaviorCache.getDailyCount(ipAddress);

        if (dailyCount >= dailyMaxRequests) {
            LOGGER.warn("IP [{}] 今日访问次数 [{}] 已达上限 [{}]", ipAddress, dailyCount, dailyMaxRequests);
            if (!dailyAlertSentSet.contains(ipAddress)) {
                dailyAlertSentSet.add(ipAddress);
                alertEmailService.sendDailyAlert(ipAddress, dailyCount);
            }
            return new RateLimitResult(false, true, "用户行为异常，不能继续访问系统，请联系管理员！");
        }

        if (periodCount > maxRequests) {
            LOGGER.warn("IP [{}] 在过去 [{}] 分钟内访问次数 [{}] 超过限制 [{}]", ipAddress, periodMinutes, periodCount, maxRequests);
            if (!periodAlertSentSet.contains(ipAddress)) {
                periodAlertSentSet.add(ipAddress);
                alertEmailService.sendPeriodAlert(ipAddress, periodMinutes, periodCount);
            }
            return new RateLimitResult(false, false, "请求太频繁，请稍后再试！");
        }

        cleanPeriodAlertIfNeeded(ipAddress, periodMinutes, maxRequests);
        return new RateLimitResult(true, false, null);
    }

    /**
     * 当请求量降到安全阈值以下时清除周期告警标记
     * 
     * <p>恢复策略：当请求数降到阈值的50%以下时清除告警标记</p>
     * <p>这样设计的目的是：</p>
     * <ul>
     *   <li>避免告警标记永远存在（即使限流已解除）</li>
     *   <li>留有一定余量（50%）避免频繁触发告警</li>
     * </ul>
     * 
     * @param ipAddress      客户端IP地址
     * @param periodMinutes  统计周期（分钟）
     * @param maxRequests    最大请求数
     */
    private void cleanPeriodAlertIfNeeded(String ipAddress, int periodMinutes, int maxRequests) {
        int count = userBehaviorCache.getPeriodCount(ipAddress, periodMinutes);
        if (count <= maxRequests * 0.5) {
            periodAlertSentSet.remove(ipAddress);
        }
    }

    /**
     * 启动每日重置任务
     * 
     * <p>每天午夜执行以下操作：</p>
     * <ol>
     *   <li>清空每日限流告警集合</li>
     *   <li>清空周期限流告警集合（可选，作为额外清理）</li>
     *   <li>重置所有IP的每日访问计数</li>
     * </ol>
     */
    private void startDailyResetTask() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT);
        long initialDelay = java.time.Duration.between(now, midnight).toMillis();

        scheduler.scheduleAtFixedRate(() -> {
            dailyAlertSentSet.clear();
            periodAlertSentSet.clear();
            userBehaviorCache.resetAllDailyCounts();
            LOGGER.info("每日限流告警缓存已重置");
        }, initialDelay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
    }

    /**
     * 优雅关闭服务
     * 
     * <p>关闭顺序：</p>
     * <ol>
     *   <li>每日重置调度器（先停止新任务）</li>
     *   <li>本地缓存清理线程</li>
     *   <li>异步数据库服务（等待队列清空）</li>
     * </ol>
     * 
     * <p>超时保护：等待5秒后强制关闭，避免无限等待</p>
     */
    public void shutdown() {
        scheduler.shutdown();
        userBehaviorCache.shutdown();
        asyncDatabaseService.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 限流检查结果封装类
     * 
     * <p>封装限流检查的三种状态信息：</p>
     * <ul>
     *   <li>allowed: 是否允许通过</li>
     *   <li>dailyBlocked: 是否为每日限流（true=每日限流，false=周期限流）</li>
     *   <li>message: 对用户显示的提示消息</li>
     * </ul>
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final boolean dailyBlocked;
        private final String message;

        /**
         * 构造函数
         * 
         * @param allowed      是否允许
         * @param dailyBlocked 是否为每日限流
         * @param message      提示消息
         */
        public RateLimitResult(boolean allowed, boolean dailyBlocked, String message) {
            this.allowed = allowed;
            this.dailyBlocked = dailyBlocked;
            this.message = message;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public boolean isDailyBlocked() {
            return dailyBlocked;
        }

        public String getMessage() {
            return message;
        }
    }
}
