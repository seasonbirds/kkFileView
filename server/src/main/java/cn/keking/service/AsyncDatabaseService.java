package cn.keking.service;

import cn.keking.model.UserBehavior;
import cn.keking.service.database.UserBehaviorDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 异步数据库服务
 * 提供异步批量数据库写入能力，减少数据库IO对请求响应的影响
 *
 * 设计思路：
 * 1. 使用阻塞队列缓存待写入的用户行为记录
 * 2. 后台线程消费队列，达到批次大小或定时触发批量写入
 * 3. 批量写入可显著减少数据库连接开销和事务次数
 */
public class AsyncDatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncDatabaseService.class);

    /** 批量插入的阈值 */
    private static final int BATCH_SIZE = 50;
    /** 定时刷新的时间间隔（毫秒） */
    private static final long FLUSH_INTERVAL_MS = 5000;

    /** 数据库操作DAO */
    private final UserBehaviorDao userBehaviorDao;
    /** 待写入记录的阻塞队列，最大容量10000条 */
    private final BlockingQueue<UserBehavior> recordQueue = new LinkedBlockingQueue<>(10000);
    /** 数据库写入线程池（单线程，保证写入顺序） */
    private final ExecutorService writerExecutor;
    /** 定时刷新线程池 */
    private final ScheduledExecutorService flushScheduler;
    /** 服务运行状态标志 */
    private volatile boolean running = true;

    /**
     * 构造函数，初始化异步数据库服务
     * @param userBehaviorDao 用户行为数据库访问对象
     */
    public AsyncDatabaseService(UserBehaviorDao userBehaviorDao) {
        this.userBehaviorDao = userBehaviorDao;
        // 创建单线程的写入器，设置为守护线程
        this.writerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "UserBehaviorDbWriter");
            t.setDaemon(true);
            return t;
        });
        // 创建单线程的定时刷新器
        this.flushScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "UserBehaviorDbFlushTimer");
            t.setDaemon(true);
            return t;
        });

        // 启动队列处理线程
        writerExecutor.submit(this::processQueue);
        // 启动定时刷新任务，每5秒强制刷新一次
        flushScheduler.scheduleAtFixedRate(this::forceFlush, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * 异步保存用户行为记录
     * 将记录放入队列，由后台线程异步写入数据库
     * @param behavior 用户行为记录
     */
    public void saveRecordAsync(UserBehavior behavior) {
        if (running) {
            // 非阻塞方式入队，队列满时丢弃并记录日志
            boolean offered = recordQueue.offer(behavior);
            if (!offered) {
                LOGGER.warn("记录队列已满，丢弃记录: IP={}, File={}", behavior.getIpAddress(), behavior.getFileName());
            }
        }
    }

    /**
     * 队列处理循环
     * 从队列中取出记录，达到批次大小时批量写入
     */
    private void processQueue() {
        List<UserBehavior> batch = new ArrayList<>(BATCH_SIZE);
        // 继续运行的条件：服务未停止或队列中还有数据
        while (running || !recordQueue.isEmpty()) {
            try {
                // 轮询方式取数据，超时100ms
                UserBehavior behavior = recordQueue.poll(100, TimeUnit.MILLISECONDS);
                if (behavior != null) {
                    batch.add(behavior);
                }

                // 达到批次大小，或者空闲时（behavior为null且batch非空）写入
                if (batch.size() >= BATCH_SIZE || (behavior == null && !batch.isEmpty())) {
                    flushBatch(batch);
                    batch.clear();
                }
            } catch (InterruptedException e) {
                // 线程被中断，退出循环
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // 捕获异常，避免单次错误导致整个服务崩溃
                LOGGER.error("处理记录队列异常", e);
            }
        }
        // 退出前尝试写入剩余数据
        if (!batch.isEmpty()) {
            flushBatch(batch);
        }
    }

    /**
     * 批量写入数据库
     * @param batch 待写入的一批数据
     */
    private void flushBatch(List<UserBehavior> batch) {
        if (batch.isEmpty()) {
            return;
        }
        try {
            userBehaviorDao.batchInsert(batch);
            LOGGER.debug("批量写入 {} 条用户行为记录", batch.size());
        } catch (Exception e) {
            LOGGER.error("批量写入数据库异常", e);
        }
    }

    /**
     * 强制刷新队列中的数据到数据库
     * 由定时任务调用，确保数据不会在队列中停留过久
     */
    private void forceFlush() {
        if (!recordQueue.isEmpty()) {
            List<UserBehavior> batch = new ArrayList<>();
            recordQueue.drainTo(batch, BATCH_SIZE);
            if (!batch.isEmpty()) {
                flushBatch(batch);
            }
        }
    }

    /**
     * 关闭服务
     * 优雅关闭线程池，等待队列中的数据写入完成
     */
    public void shutdown() {
        running = false;
        flushScheduler.shutdown();
        writerExecutor.shutdown();
        try {
            // 等待写入线程完成，最多等10秒
            if (!writerExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                writerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            writerExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
