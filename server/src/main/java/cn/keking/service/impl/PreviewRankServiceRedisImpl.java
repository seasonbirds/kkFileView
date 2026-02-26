package cn.keking.service.impl;

import cn.keking.service.PreviewRankService;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 预览排行榜服务的Redis实现
 * 使用Redis Sorted Set实现排行榜功能，支持高并发下的原子计数
 */
@Service
public class PreviewRankServiceRedisImpl implements PreviewRankService {

    /**
     * Redis中存储文件名与URL映射的Key
     */
    private static final String PREVIEW_FILE_URL_MAP_KEY = "file-preview-url-map";

    private final RedissonClient redissonClient;

    /**
     * 构造函数，注入预览排行榜专用的RedissonClient
     *
     * @param redissonClient 预览排行榜专用的Redis客户端
     */
    public PreviewRankServiceRedisImpl(@Qualifier("previewRankRedissonClient") RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 增加文件的预览次数
     * 使用@Async注解实现异步处理，不影响主线程响应时间
     * 使用Redis ZINCRBY原子操作保证并发安全
     *
     * @param fileName 文件名称
     * @param fileUrl  文件URL地址
     */
    @Override
    @Async
    public void incrementPreviewCount(String fileName, String fileUrl) {
        // 获取排行榜Sorted Set，score为预览次数，member为文件名
        RScoredSortedSet<String> rankSet = redissonClient.getScoredSortedSet(PREVIEW_RANK_KEY);
        // 原子增加预览次数，ZINCRBY操作保证并发安全
        rankSet.addScore(fileName, 1);
        // 存储文件名与URL的映射关系，用于后续查询
        RMap<String, String> urlMap = redissonClient.getMap(PREVIEW_FILE_URL_MAP_KEY);
        urlMap.putIfAbsent(fileName, fileUrl);
    }

    /**
     * 获取预览次数排行榜
     * 按预览次数降序排列，返回前N名
     *
     * @param topN 返回的记录数量
     * @return 排行榜列表
     */
    @Override
    public List<PreviewRankItem> getTopRankList(int topN) {
        // 获取排行榜Sorted Set
        RScoredSortedSet<String> rankSet = redissonClient.getScoredSortedSet(PREVIEW_RANK_KEY);
        // 获取文件名与URL映射
        RMap<String, String> urlMap = redissonClient.getMap(PREVIEW_FILE_URL_MAP_KEY);
        
        List<PreviewRankItem> result = new ArrayList<>();
        int rank = 1;
        int count = 0;
        
        // 按分数降序遍历，获取前topN名
        for (String fileName : rankSet.valueRangeReversed(0, topN - 1)) {
            // 获取预览次数（分数）
            Double score = rankSet.getScore(fileName);
            if (score == null) {
                score = 0.0;
            }
            // 获取文件URL
            String fileUrl = urlMap.get(fileName);
            // 构建排行榜条目
            result.add(new PreviewRankItem(rank, fileName, score.longValue(), fileUrl));
            rank++;
            count++;
            if (count >= topN) {
                break;
            }
        }
        
        return result;
    }
}
