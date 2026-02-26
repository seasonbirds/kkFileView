package cn.keking.service.impl;

import cn.keking.service.FilePreviewStatisticsService;
import org.redisson.Redisson;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件预览统计服务实现类
 * 基于Redis Sorted Set实现排行榜功能
 *
 * @author kl
 * @date 2026/02/25
 */
@Service
@ConditionalOnBean(name = "rankingRedissonConfig")
public class FilePreviewStatisticsServiceImpl implements FilePreviewStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(FilePreviewStatisticsServiceImpl.class);

    private final Config config;
    private RedissonClient redissonClient;

    @Autowired
    public FilePreviewStatisticsServiceImpl(@Qualifier("rankingRedissonConfig") Config config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        try {
            this.redissonClient = Redisson.create(config);
            logger.info("排行榜Redis客户端初始化成功");
        } catch (Exception e) {
            logger.error("排行榜Redis客户端初始化失败", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (redissonClient != null && !redissonClient.isShutdown()) {
            redissonClient.shutdown();
            logger.info("排行榜Redis客户端已关闭");
        }
    }

    /**
     * 增加文件预览次数
     * 使用Redis Sorted Set的incrementScore方法，确保并发安全
     *
     * @param fileName 文件名
     * @param fileUrl  文件URL
     */
    @Override
    public void incrementPreviewCount(String fileName, String fileUrl) {
        try {
            if (redissonClient == null || fileUrl == null || fileUrl.trim().isEmpty()) {
                return;
            }
            // 使用文件URL作为唯一标识，文件名用于展示
            String member = fileName + "|||" + fileUrl;
            RScoredSortedSet<String> rankingSet = redissonClient.getScoredSortedSet(FILE_PREVIEW_RANKING_KEY);
            // incrementScore方法是原子操作，确保并发安全
            rankingSet.addScore(member, 1);
            logger.debug("文件预览次数增加: {}, 文件名: {}", fileUrl, fileName);
        } catch (Exception e) {
            logger.error("增加文件预览次数失败: {}, 文件名: {}", fileUrl, fileName, e);
        }
    }

    /**
     * 获取文件预览排行榜
     * 按预览次数降序排列
     *
     * @param topN 前N名
     * @return 排行榜列表
     */
    @Override
    public List<Map<String, Object>> getPreviewRanking(int topN) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            if (redissonClient == null) {
                return result;
            }
            RScoredSortedSet<String> rankingSet = redissonClient.getScoredSortedSet(FILE_PREVIEW_RANKING_KEY);
            // 按分数降序获取前N名（分数越高，排名越靠前）
            Collection<String> topMembers = rankingSet.valueRangeReversed(0, topN - 1);

            int rank = 1;
            for (String member : topMembers) {
                Map<String, Object> item = new HashMap<>();
                String[] parts = member.split("\\|\\|\\|", 2);
                String fileName = parts.length > 0 ? parts[0] : "";
                String fileUrl = parts.length > 1 ? parts[1] : member;

                Double score = rankingSet.getScore(member);
                long count = score != null ? score.longValue() : 0;

                item.put("rank", rank++);
                item.put("fileName", fileName);
                item.put("fileUrl", fileUrl);
                item.put("previewCount", count);
                result.add(item);
            }
        } catch (Exception e) {
            logger.error("获取文件预览排行榜失败", e);
        }
        return result;
    }

    /**
     * 获取指定文件的预览次数
     *
     * @param fileUrl 文件URL
     * @return 预览次数
     */
    @Override
    public Long getPreviewCount(String fileUrl) {
        try {
            if (redissonClient == null || fileUrl == null || fileUrl.trim().isEmpty()) {
                return 0L;
            }
            RScoredSortedSet<String> rankingSet = redissonClient.getScoredSortedSet(FILE_PREVIEW_RANKING_KEY);
            // 需要遍历查找，因为member包含文件名和URL
            for (String member : rankingSet) {
                if (member.endsWith("|||" + fileUrl)) {
                    Double score = rankingSet.getScore(member);
                    return score != null ? score.longValue() : 0L;
                }
            }
        } catch (Exception e) {
            logger.error("获取文件预览次数失败: {}", fileUrl, e);
        }
        return 0L;
    }
}
