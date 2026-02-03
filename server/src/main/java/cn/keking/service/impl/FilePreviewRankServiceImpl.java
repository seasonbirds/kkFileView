package cn.keking.service.impl;

import cn.keking.service.FilePreviewRankService;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件预览排行榜服务实现类（Redis版）
 * 基于Redis Sorted Set实现高性能排行榜
 * <p>
 * 高并发场景下的计数准确性保证：
 * 1. 使用Redis Sorted Set的incrementScore方法，该方法是原子操作
 * 2. Redis单线程执行命令，天然保证并发安全
 * 3. 无需加锁，性能最优
 *
 * @author kl
 * @since 2024/01/01
 */
@Service
public class FilePreviewRankServiceImpl implements FilePreviewRankService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePreviewRankServiceImpl.class);

    private final RedissonClient redissonClient;

    public FilePreviewRankServiceImpl(@Qualifier("rankRedissonClient") RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 增加文件预览次数
     * 使用Redis Sorted Set的incrementScore原子方法，确保高并发场景下计数准确
     *
     * @param fileName 文件名称
     */
    @Override
    public void incrementPreviewCount(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }
        try {
            RScoredSortedSet<String> rankSet = redissonClient.getScoredSortedSet(FILE_PREVIEW_RANK_KEY);
            // incrementScore是原子操作，Redis单线程执行，天然线程安全
            rankSet.addScore(fileName, 1);
        } catch (Exception e) {
            LOGGER.error("增加文件预览次数失败，fileName：{}，error：{}", fileName, e.getMessage());
        }
    }

    /**
     * 获取文件预览排行榜
     *
     * @param topN 前N名
     * @return 排行榜列表
     */
    @Override
    public List<Map<String, Object>> getTopN(int topN) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            RScoredSortedSet<String> rankSet = redissonClient.getScoredSortedSet(FILE_PREVIEW_RANK_KEY);
            // 按分数降序获取前N名
            Collection<String> topMembers = rankSet.valueRangeReversed(0, topN - 1);

            int rank = 1;
            for (String fileName : topMembers) {
                Map<String, Object> item = new HashMap<>();
                Double score = rankSet.getScore(fileName);
                item.put("rank", rank++);
                item.put("fileName", fileName);
                item.put("previewCount", score != null ? score.longValue() : 0);
                result.add(item);
            }
        } catch (Exception e) {
            LOGGER.error("获取文件预览排行榜失败，error：{}", e.getMessage());
        }
        return result;
    }

    /**
     * 获取文件预览次数
     *
     * @param fileName 文件名称
     * @return 预览次数
     */
    @Override
    public long getPreviewCount(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return 0;
        }
        try {
            RScoredSortedSet<String> rankSet = redissonClient.getScoredSortedSet(FILE_PREVIEW_RANK_KEY);
            Double score = rankSet.getScore(fileName);
            return score != null ? score.longValue() : 0;
        } catch (Exception e) {
            LOGGER.error("获取文件预览次数失败，fileName：{}，error：{}", fileName, e.getMessage());
            return 0;
        }
    }

    /**
     * 清除排行榜数据
     */
    @Override
    public void clearRank() {
        try {
            redissonClient.getScoredSortedSet(FILE_PREVIEW_RANK_KEY).clear();
        } catch (Exception e) {
            LOGGER.error("清除排行榜数据失败，error：{}", e.getMessage());
        }
    }
}
