package cn.keking.service.impl;

import cn.keking.service.PreviewCountService;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 文件预览次数统计服务Redis实现
 * @author kkfileview
 */
@Service
public class PreviewCountServiceRedisImpl implements PreviewCountService {
    
    private static final Logger logger = LoggerFactory.getLogger(PreviewCountServiceRedisImpl.class);
    
    // Redis中存储预览次数的有序集合键名
    private static final String PREVIEW_COUNT_KEY = "kkfileview:preview:count";
    
    private final RedissonClient previewCountRedissonClient;
    
    public PreviewCountServiceRedisImpl(@Qualifier("previewCountRedissonClient") RedissonClient previewCountRedissonClient) {
        this.previewCountRedissonClient = previewCountRedissonClient;
    }
    
    @Override
    public void incrementPreviewCount(String fileUrl) {
        // 使用异步方式增加预览次数，避免影响主流程性能
        CompletableFuture.runAsync(() -> {
            try {
                RScoredSortedSet<String> previewCountSet = previewCountRedissonClient.getScoredSortedSet(PREVIEW_COUNT_KEY);
                // 使用incrementScore原子操作确保并发安全
                previewCountSet.addScore(fileUrl, 1);
                logger.debug("文件预览次数增加: {}", fileUrl);
            } catch (Exception e) {
                logger.error("增加文件预览次数失败: {}", fileUrl, e);
            }
        });
    }
    
    @Override
    public long getPreviewCount(String fileUrl) {
        try {
            RScoredSortedSet<String> previewCountSet = previewCountRedissonClient.getScoredSortedSet(PREVIEW_COUNT_KEY);
            Double score = previewCountSet.getScore(fileUrl);
            return score == null ? 0 : score.longValue();
        } catch (Exception e) {
            logger.error("获取文件预览次数失败: {}", fileUrl, e);
            return 0;
        }
    }
    
    @Override
    public List<Map<String, Object>> getTopFiles(int topN) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            RScoredSortedSet<String> previewCountSet = previewCountRedissonClient.getScoredSortedSet(PREVIEW_COUNT_KEY);
            // 获取预览次数最多的前N个文件，按分数降序排列
            // 使用entryRange获取分数和成员，然后按分数降序排序
            List<Map.Entry<String, Double>> entries = previewCountSet.entryRange(0, -1);
            
            if (entries != null && !entries.isEmpty()) {
                // 按分数降序排序
                entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
                
                // 取前N个
                int limit = Math.min(topN, entries.size());
                for (int i = 0; i < limit; i++) {
                    Map.Entry<String, Double> entry = entries.get(i);
                    String fileUrl = entry.getKey();
                    Double score = entry.getValue();
                    
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("rank", i + 1);
                    fileInfo.put("fileUrl", fileUrl);
                    fileInfo.put("fileName", getFileNameFromUrl(fileUrl));
                    fileInfo.put("previewCount", score == null ? 0 : score.longValue());
                    result.add(fileInfo);
                }
            }
        } catch (Exception e) {
            logger.error("获取排行榜失败", e);
        }
        return result;
    }
    
    @Override
    public String getFileNameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "";
        }
        
        // 处理URL编码
        String decodedUrl = fileUrl;
        try {
            decodedUrl = java.net.URLDecoder.decode(fileUrl, "UTF-8");
        } catch (Exception e) {
            logger.debug("URL解码失败: {}", fileUrl, e);
        }
        
        // 提取文件名
        int lastSlashIndex = Math.max(decodedUrl.lastIndexOf('/'), decodedUrl.lastIndexOf('\\'));
        if (lastSlashIndex >= 0 && lastSlashIndex < decodedUrl.length() - 1) {
            return decodedUrl.substring(lastSlashIndex + 1);
        }
        
        return decodedUrl;
    }
}