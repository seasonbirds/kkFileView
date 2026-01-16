package cn.keking.service.impl;

import cn.keking.service.FilePreviewStatisticsService;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 文件预览统计服务实现类
 * 基于Redis Sorted Set实现高效的文件预览次数统计和排名功能
 * 
 * 实现原理：
 * 1. 使用Redis Sorted Set存储文件预览次数，成员为文件名，分数为预览次数
 * 2. 每次预览时，通过addScore方法原子性地增加对应文件的分数
 * 3. 查询排行榜时，通过valueRangeReversed方法按分数降序获取Top N文件
 * 
 * 优势：
 * - 原子操作：Redis的addScore操作是原子的，确保高并发场景下计数准确
 * - 高效排序：Sorted Set天然支持按分数排序，查询Top N性能优异
 * - 内存占用小：只存储文件名和预览次数，不存储文件URL等额外信息
 * 
 * @author kkfileview
 */
@ConditionalOnProperty(name = "file.preview.statistics.enabled", havingValue = "true", matchIfMissing = true)
@Service
public class FilePreviewStatisticsServiceImpl implements FilePreviewStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(FilePreviewStatisticsServiceImpl.class);

    private final RedissonClient redissonClient;

    public FilePreviewStatisticsServiceImpl(@Qualifier("statisticsRedissonClient") RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void incrementPreviewCount(String fileName) {
        try {
            RScoredSortedSet<String> previewCountSet = redissonClient.getScoredSortedSet(PREVIEW_COUNT_KEY);
            
            if (fileName == null || fileName.isEmpty()) {
                logger.warn("文件名为空，无法统计预览次数");
                return;
            }
            
            previewCountSet.addScore(fileName, 1);
            
            logger.debug("文件预览统计成功，fileName: {}", fileName);
        } catch (Exception e) {
            logger.error("文件预览统计失败，fileName: {}", fileName, e);
        }
    }

    @Override
    public List<Map<String, Object>> getTopPreviewFiles(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            RScoredSortedSet<String> previewCountSet = redissonClient.getScoredSortedSet(PREVIEW_COUNT_KEY);
            
            Collection<String> topFiles = previewCountSet.valueRangeReversed(0, limit - 1);
            int rank = 1;
            
            for (String fileName : topFiles) {
                Double score = previewCountSet.getScore(fileName);
                if (score != null) {
                    Map<String, Object> fileInfo = Map.of(
                        "rank", rank++,
                        "fileName", fileName,
                        "previewCount", score.longValue()
                    );
                    result.add(fileInfo);
                }
            }
            
            logger.debug("获取文件预览排行榜成功，limit: {}, resultSize: {}", limit, result.size());
        } catch (Exception e) {
            logger.error("获取文件预览排行榜失败", e);
        }
        return result;
    }

    @Override
    public void clearStatistics() {
        try {
            redissonClient.getScoredSortedSet(PREVIEW_COUNT_KEY).delete();
            logger.info("文件预览统计数据已清空");
        } catch (Exception e) {
            logger.error("清空文件预览统计数据失败", e);
        }
    }
}