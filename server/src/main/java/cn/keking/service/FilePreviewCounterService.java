package cn.keking.service;

import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 文件预览次数统计服务
 * 使用Redis的Sorted Set实现排行榜功能
 * 使用实时更新策略，确保数据不丢失
 */
@Service
public class FilePreviewCounterService {

    private static final Logger logger = LoggerFactory.getLogger(FilePreviewCounterService.class);

    /**
     * Redis中存储排行榜的Key
     */
    public static final String FILE_PREVIEW_RANKING_KEY = "file:preview:ranking";

    private final RedissonClient redissonClient;

    public FilePreviewCounterService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        logger.info("FilePreviewCounterService initialized with real-time update strategy");
    }

    /**
     * 增加文件预览次数
     * 实时更新到Redis，确保数据不丢失
     *
     * @param fileName 文件名
     */
    public void incrementPreviewCount(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return;
        }
        try {
            RScoredSortedSet<String> sortedSet = redissonClient.getScoredSortedSet(FILE_PREVIEW_RANKING_KEY);
            // 使用增量更新，原子操作
            sortedSet.addScore(fileName, 1.0);
            logger.debug("Incremented preview count for file: {}", fileName);
        } catch (Exception e) {
            logger.error("Failed to increment preview count for file: {}", fileName, e);
        }
    }

    /**
     * 获取预览次数排行榜
     *
     * @param topN 获取前N名
     * @return 排行榜列表，每个元素包含文件名和预览次数
     */
    public List<Map<String, Object>> getTopFiles(int topN) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            RScoredSortedSet<String> sortedSet = redissonClient.getScoredSortedSet(FILE_PREVIEW_RANKING_KEY);
            
            // 从高到低获取前N名
            Map<String, Double> topScores = sortedSet.entryRangeReversed(0, topN - 1);
            
            int rank = 1;
            for (Map.Entry<String, Double> entry : topScores.entrySet()) {
                Map<String, Object> item = new ConcurrentHashMap<>();
                item.put("rank", rank++);
                item.put("fileName", entry.getKey());
                item.put("previewCount", entry.getValue().longValue());
                result.add(item);
            }
            
            logger.debug("Retrieved top {} files from ranking", result.size());
        } catch (Exception e) {
            logger.error("Failed to get top files ranking from Redis", e);
        }
        
        return result;
    }

    /**
     * 获取指定文件的预览次数
     *
     * @param fileName 文件名
     * @return 预览次数
     */
    public long getPreviewCount(String fileName) {
        try {
            RScoredSortedSet<String> sortedSet = redissonClient.getScoredSortedSet(FILE_PREVIEW_RANKING_KEY);
            Double score = sortedSet.getScore(fileName);
            return score != null ? score.longValue() : 0L;
        } catch (Exception e) {
            logger.error("Failed to get preview count for file: {}", fileName, e);
            return 0L;
        }
    }
}
