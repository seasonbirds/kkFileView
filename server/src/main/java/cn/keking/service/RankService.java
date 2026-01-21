package cn.keking.service;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文件预览排行榜服务
 * 用于统计文件预览次数和生成排行榜
 */
@Service
public class RankService {

    private static final String FILE_PREVIEW_COUNT_KEY = "file-preview-count";
    private final RedissonClient rankRedissonClient;
    private final Map<String, Long> localCountMap = new ConcurrentHashMap<>();

    public RankService(@Qualifier("rankRedissonClient") RedissonClient rankRedissonClient) {
        this.rankRedissonClient = rankRedissonClient;
    }

    /**
     * 增加文件预览次数
     * @param fileName 文件名
     */
    public void incrementFilePreviewCount(String fileName) {
        if (rankRedissonClient != null) {
            // 使用Redis进行统计
            rankRedissonClient.getAtomicLong(FILE_PREVIEW_COUNT_KEY + ":" + fileName).incrementAndGet();
        } else {
            // 使用本地Map进行统计（当Redis不可用时）
            localCountMap.compute(fileName, (k, v) -> v == null ? 1 : v + 1);
        }
    }

    /**
     * 获取文件预览排行榜
     * @param topN 排行榜数量
     * @return 文件名和预览次数的映射
     */
    public Map<String, Long> getFilePreviewTop(int topN) {
        Map<String, Long> result = new LinkedHashMap<>();
        
        if (rankRedissonClient != null) {
            // 使用Redis获取排行榜
            try {
                // 获取所有以FILE_PREVIEW_COUNT_KEY为前缀的key
                Iterable<String> keys = rankRedissonClient.getKeys().getKeysByPattern(FILE_PREVIEW_COUNT_KEY + ":*");
                // 创建一个临时的有序集合
                String tempZSetKey = "temp:file-preview-top";
                org.redisson.api.RScoredSortedSet<String> zSet = rankRedissonClient.getScoredSortedSet(tempZSetKey);
                
                // 将所有文件的预览次数添加到有序集合中
                for (String key : keys) {
                    String fileName = key.substring((FILE_PREVIEW_COUNT_KEY + ":").length());
                    long count = rankRedissonClient.getAtomicLong(key).get();
                    zSet.add(count, fileName);
                }
                
                // 获取前topN个元素
                java.util.List<String> topFiles = zSet.reverseRange(0, topN - 1);
                for (String fileName : topFiles) {
                    Double score = zSet.getScore(fileName);
                    if (score != null) {
                        result.put(fileName, score.longValue());
                    }
                }
                
                // 删除临时有序集合
                zSet.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // 使用本地Map获取排行榜（当Redis不可用时）
            localCountMap.entrySet()
                    .stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .limit(topN)
                    .forEach(e -> result.put(e.getKey(), e.getValue()));
        }
        
        return result;
    }
}