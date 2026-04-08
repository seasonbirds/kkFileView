package cn.keking.service;

import cn.keking.config.RankRedisConfig;
import org.redisson.Redisson;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件预览排行榜服务类
 * 基于Redis的Sorted Set实现文件预览次数统计和排行榜功能
 *
 * 实现说明：
 * 1. 使用Redis的Sorted Set数据结构，这是实现排行榜的最优方式
 * 2. 预览次数统计使用异步操作，不阻塞主流程
 * 3. Redis操作天然支持原子性，保证高并发场景下的线程安全
 *
 * @author kkFileView
 */
@Service
public class RankService {

    private static final Logger logger = LoggerFactory.getLogger(RankService.class);

    private static final String RANK_KEY = "kkFileView:file:preview:rank";
    private static final String FILE_NAME_PREFIX = "kkFileView:file:name:";

    private final RankRedisConfig rankRedisConfig;
    private RedissonClient redissonClient;

    public RankService(RankRedisConfig rankRedisConfig) {
        this.rankRedisConfig = rankRedisConfig;
    }

    /**
     * 初始化Redis客户端
     * 在服务启动时创建Redisson连接
     */
    @PostConstruct
    public void init() {
        try {
            Config config = rankRedisConfig.rankRedisConfig();
            this.redissonClient = Redisson.create(config);
            logger.info("Rank Redis client initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Rank Redis client: {}", e.getMessage());
        }
    }

    /**
     * 销毁Redis客户端
     * 在服务关闭时释放Redisson连接
     */
    @PreDestroy
    public void destroy() {
        if (redissonClient != null) {
            redissonClient.shutdown();
            logger.info("Rank Redis client shutdown");
        }
    }

    /**
     * 增加文件预览次数
     * 使用Redis的Sorted Set原子操作，天然支持高并发
     * 使用异步操作，不影响核心预览接口性能
     *
     * @param fileUrl 文件URL（作为唯一标识）
     * @param fileName 文件名称（用于显示）
     */
    public void incrementPreviewCount(String fileUrl, String fileName) {
        if (redissonClient == null) {
            return;
        }
        try {
            RScoredSortedSet<String> sortedSet = redissonClient.getScoredSortedSet(RANK_KEY);
            sortedSet.addScoreAsync(fileUrl, 1.0);
            if (fileName != null && !fileName.isEmpty()) {
                redissonClient.getBucket(FILE_NAME_PREFIX + fileUrl).setAsync(fileName);
            }
        } catch (Exception e) {
            logger.error("Failed to increment preview count for file: {}", fileUrl, e);
        }
    }

    /**
     * 获取最受欢迎文件排行榜
     * 按预览次数从高到低排序
     *
     * @param topN 获取前N名
     * @return 排行榜列表
     */
    public List<RankItem> getTopFiles(int topN) {
        List<RankItem> result = new ArrayList<>();
        if (redissonClient == null) {
            return result;
        }
        try {
            RScoredSortedSet<String> sortedSet = redissonClient.getScoredSortedSet(RANK_KEY);
            var entries = sortedSet.entryRangeReversed(0, topN - 1);
            int rank = 1;
            for (var entry : entries) {
                String fileUrl = entry.getValue();
                double score = entry.getScore();
                String fileName = getFileName(fileUrl);
                result.add(new RankItem(rank++, fileUrl, fileName, (long) score));
            }
        } catch (Exception e) {
            logger.error("Failed to get top files: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 获取文件名称
     * 优先从Redis中获取存储的文件名，如果没有则从URL中提取
     *
     * @param fileUrl 文件URL
     * @return 文件名称
     */
    private String getFileName(String fileUrl) {
        if (redissonClient == null) {
            return extractFileNameFromUrl(fileUrl);
        }
        try {
            String fileName = (String) redissonClient.getBucket(FILE_NAME_PREFIX + fileUrl).get();
            if (fileName != null && !fileName.isEmpty()) {
                return fileName;
            }
        } catch (Exception e) {
            logger.error("Failed to get file name for url: {}", fileUrl, e);
        }
        return extractFileNameFromUrl(fileUrl);
    }

    /**
     * 从URL中提取文件名
     *
     * @param fileUrl 文件URL
     * @return 提取的文件名
     */
    private String extractFileNameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "unknown";
        }
        try {
            int lastSlash = fileUrl.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < fileUrl.length() - 1) {
                return fileUrl.substring(lastSlash + 1);
            }
        } catch (Exception e) {
            logger.error("Failed to extract file name from url: {}", fileUrl, e);
        }
        return fileUrl;
    }

    /**
     * 排行榜数据项
     */
    public static class RankItem {
        private int rank;
        private String fileUrl;
        private String fileName;
        private long previewCount;

        public RankItem(int rank, String fileUrl, String fileName, long previewCount) {
            this.rank = rank;
            this.fileUrl = fileUrl;
            this.fileName = fileName;
            this.previewCount = previewCount;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getPreviewCount() {
            return previewCount;
        }

        public void setPreviewCount(long previewCount) {
            this.previewCount = previewCount;
        }
    }
}
