package cn.keking.service.userbehavior;

import cn.keking.config.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用户行为访问管理器
 * 负责管理IP访问限制、统计和告警的核心逻辑
 *
 * 核心功能：
 * 1. 使用本地缓存实现访问频率统计（滑动窗口算法）
 * 2. 管理周期限制（X分钟内Y次请求）和日限制
 * 3. 异步处理数据库记录和邮件发送
 * 4. 提供IP限制状态查询和清理接口
 */
public class UserBehaviorAccessManager {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorAccessManager.class);

    /**
     * 周期限制IP集合（临时限制，到期自动解除）
     * 使用ConcurrentHashMap.newKeySet()保证线程安全
     */
    private static final Set<String> blockedPeriodIps = ConcurrentHashMap.newKeySet();

    /**
     * 周期限制时间记录，key=IP，value=限制到期时间戳
     */
    private static final Map<String, Long> periodBlockTime = new ConcurrentHashMap<>();

    /**
     * 日限制IP集合（当日有效，每日定时清理）
     */
    private static final Set<String> blockedDailyIps = ConcurrentHashMap.newKeySet();

    /**
     * 每日请求计数器，key=IP，value=请求次数（原子整数）
     */
    private static final Map<String, AtomicInteger> dailyRequestCount = new ConcurrentHashMap<>();

    /**
     * 周期请求时间戳缓存，用于滑动窗口算法
     * key=IP，value=请求时间戳列表
     */
    private static final Map<String, List<Long>> periodRequestCache = new ConcurrentHashMap<>();

    /**
     * 异步执行线程池
     * 用于处理数据库记录和邮件发送等耗时操作，避免阻塞主请求线程
     * 守护线程，随JVM退出自动终止
     */
    private static final ExecutorService asyncExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "UserBehaviorMonitor-Async");
        t.setDaemon(true);
        return t;
    });

    /**
     * 记录用户行为并检查访问限制
     * 这是核心入口方法，依次执行：
     * 1. 异步记录行为到数据库
     * 2. 更新本地缓存计数
     * 3. 检查是否触发限制条件
     *
     * @param ipAddress          客户端IP地址
     * @param fileName           访问的文件名
     * @param userBehaviorService 用户行为服务（数据库操作）
     * @param alertEmailService  告警邮件服务
     * @param result             检查结果输出对象
     */
    public static void recordAndCheck(String ipAddress, String fileName,
                                       UserBehaviorService userBehaviorService,
                                       AlertEmailService alertEmailService,
                                       AccessCheckResult result) {
        asyncRecordBehavior(userBehaviorService, ipAddress, fileName);
        incrementRequestCache(ipAddress);
        checkAndBlock(ipAddress, alertEmailService, result);
    }

    /**
     * 检查IP是否被限制（包括周期限制和日限制）
     *
     * @param ipAddress 客户端IP地址
     * @return true表示被限制，false表示允许访问
     */
    public static boolean isBlocked(String ipAddress) {
        return isBlockedDaily(ipAddress) || isBlockedPeriod(ipAddress);
    }

    /**
     * 检查IP是否被日限制
     *
     * @param ipAddress 客户端IP地址
     * @return true表示被日限制
     */
    public static boolean isBlockedDaily(String ipAddress) {
        return blockedDailyIps.contains(ipAddress);
    }

    /**
     * 检查IP是否被周期限制（临时限制）
     * 如果限制已过期，自动解除限制并返回false
     *
     * @param ipAddress 客户端IP地址
     * @return true表示仍在周期限制中
     */
    public static boolean isBlockedPeriod(String ipAddress) {
        if (!blockedPeriodIps.contains(ipAddress)) {
            return false;
        }
        Long blockedUntil = periodBlockTime.get(ipAddress);
        if (blockedUntil == null || System.currentTimeMillis() > blockedUntil) {
            blockedPeriodIps.remove(ipAddress);
            periodBlockTime.remove(ipAddress);
            return false;
        }
        return true;
    }

    /**
     * 清理当日数据（由定时任务调用）
     * 每日凌晨清理：日限制IP集合、日请求计数、周期请求缓存
     */
    public static void clearDailyData() {
        blockedDailyIps.clear();
        dailyRequestCount.clear();
        periodRequestCache.clear();
        logger.info("已清除当日IP限制列表和请求计数缓存");
    }

    /**
     * 关闭异步线程池（由Filter.destroy()调用）
     * 先尝试优雅关闭，等待5秒后强制关闭
     */
    public static void shutdown() {
        try {
            asyncExecutor.shutdown();
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 异步记录用户行为到数据库
     * 使用线程池异步执行，不阻塞主请求
     *
     * @param service   用户行为服务
     * @param ipAddress 客户端IP
     * @param fileName  文件名
     */
    private static void asyncRecordBehavior(UserBehaviorService service, String ipAddress, String fileName) {
        asyncExecutor.submit(() -> {
            try {
                if (service != null) {
                    service.recordBehavior(ipAddress, fileName);
                }
            } catch (Exception e) {
                logger.error("异步记录用户行为失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 更新请求缓存计数
     * 1. 将当前请求时间戳添加到周期请求缓存（滑动窗口）
     * 2. 日请求计数器加1（原子操作）
     *
     * @param ipAddress 客户端IP
     */
    private static void incrementRequestCache(String ipAddress) {
        periodRequestCache.computeIfAbsent(ipAddress, k -> new CopyOnWriteArrayList<>())
                .add(System.currentTimeMillis());
        dailyRequestCount.computeIfAbsent(ipAddress, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    /**
     * 检查并设置访问限制
     * 依次检查周期限制和日限制，将结果写入result对象
     *
     * @param ipAddress         客户端IP
     * @param alertEmailService 告警邮件服务
     * @param result            检查结果输出对象
     */
    private static void checkAndBlock(String ipAddress, AlertEmailService alertEmailService, AccessCheckResult result) {
        boolean periodBlocked = checkPeriodLimit(ipAddress, alertEmailService);
        boolean dailyBlocked = checkDailyLimit(ipAddress, alertEmailService);

        result.setPeriodBlocked(periodBlocked);
        result.setDailyBlocked(dailyBlocked);
    }

    /**
     * 检查周期限制（滑动窗口算法）
     * 1. 计算周期起始时间点（当前时间 - 配置的周期分钟数）
     * 2. 统计周期内的请求次数（过滤过期时间戳）
     * 3. 清理过期的时间戳（避免内存泄漏）
     * 4. 如果超过阈值，添加到限制集合，发送告警邮件
     *
     * @param ipAddress         客户端IP
     * @param alertEmailService 告警邮件服务
     * @return true表示触发周期限制
     */
    private static boolean checkPeriodLimit(String ipAddress, AlertEmailService alertEmailService) {
        int periodMinutes = ConfigConstants.getUserBehaviorStatisticsPeriodMinutes();
        int maxRequests = ConfigConstants.getUserBehaviorStatisticsMaxRequests();

        try {
            List<Long> timestamps = periodRequestCache.get(ipAddress);
            if (timestamps == null) {
                return false;
            }

            long periodStartTime = System.currentTimeMillis() - (long) periodMinutes * 60 * 1000;
            long count = timestamps.stream().filter(t -> t >= periodStartTime).count();

            timestamps.removeIf(t -> t < periodStartTime);

            if (count > maxRequests) {
                blockedPeriodIps.add(ipAddress);
                long blockUntil = System.currentTimeMillis() + periodMinutes * 60 * 1000L;
                periodBlockTime.put(ipAddress, blockUntil);

                asyncSendAbnormalAccessAlert(alertEmailService, ipAddress, count, periodMinutes, maxRequests);

                logger.warn("IP {} 在 {} 分钟内请求 {} 次，超过阈值 {}，已临时限制访问",
                        ipAddress, periodMinutes, count, maxRequests);
                return true;
            }
        } catch (Exception e) {
            logger.error("检查周期限制失败: {}", e.getMessage());
        }

        return false;
    }

    /**
     * 异步发送周期访问超限告警邮件
     *
     * @param service       告警邮件服务
     * @param ipAddress     客户端IP
     * @param count         请求次数
     * @param periodMinutes 周期分钟数
     * @param maxRequests   最大允许请求数
     */
    private static void asyncSendAbnormalAccessAlert(AlertEmailService service, String ipAddress,
                                                     long count, int periodMinutes, int maxRequests) {
        asyncExecutor.submit(() -> {
            try {
                if (service != null) {
                    service.sendAbnormalAccessAlert(ipAddress, count, periodMinutes, maxRequests);
                }
            } catch (Exception e) {
                logger.error("异步发送周期访问告警邮件失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 检查日限制
     * 1. 获取当日请求次数（从本地缓存读取，避免数据库查询）
     * 2. 如果超过日阈值，添加到日限制集合，发送告警邮件
     *
     * @param ipAddress         客户端IP
     * @param alertEmailService 告警邮件服务
     * @return true表示触发日限制
     */
    private static boolean checkDailyLimit(String ipAddress, AlertEmailService alertEmailService) {
        int dailyMaxRequests = ConfigConstants.getUserBehaviorDailyMaxRequests();

        try {
            AtomicInteger count = dailyRequestCount.get(ipAddress);
            if (count == null) {
                return false;
            }

            if (count.get() > dailyMaxRequests) {
                blockedDailyIps.add(ipAddress);

                asyncSendDailyLimitAlert(alertEmailService, ipAddress, count.get(), dailyMaxRequests);

                logger.warn("IP {} 今日请求 {} 次，超过日阈值 {}，已限制今日访问",
                        ipAddress, count.get(), dailyMaxRequests);
                return true;
            }
        } catch (Exception e) {
            logger.error("检查日限制失败: {}", e.getMessage());
        }

        return false;
    }

    /**
     * 异步发送日限制超限告警邮件
     *
     * @param service         告警邮件服务
     * @param ipAddress       客户端IP
     * @param count           请求次数
     * @param dailyMaxRequests 日最大允许请求数
     */
    private static void asyncSendDailyLimitAlert(AlertEmailService service, String ipAddress,
                                                  int count, int dailyMaxRequests) {
        asyncExecutor.submit(() -> {
            try {
                if (service != null) {
                    service.sendDailyLimitExceededAlert(ipAddress, count, dailyMaxRequests);
                }
            } catch (Exception e) {
                logger.error("异步发送日限制告警邮件失败: {}", e.getMessage());
            }
        });
    }

    /**
     * 访问检查结果封装类
     * 用于将检查结果传递给调用方
     */
    public static class AccessCheckResult {
        private boolean periodBlocked;
        private boolean dailyBlocked;

        public boolean isPeriodBlocked() {
            return periodBlocked;
        }

        public void setPeriodBlocked(boolean periodBlocked) {
            this.periodBlocked = periodBlocked;
        }

        public boolean isDailyBlocked() {
            return dailyBlocked;
        }

        public void setDailyBlocked(boolean dailyBlocked) {
            this.dailyBlocked = dailyBlocked;
        }

        public boolean isBlocked() {
            return periodBlocked || dailyBlocked;
        }
    }
}