package cn.keking.service.cache.impl;

import cn.keking.service.cache.CacheService;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件排行榜缓存服务
 * 专门用于文件排行榜功能，总是被实例化，使用专门的Redis客户端
 */
@Service("fileRankCacheService")
public class FileRankCacheService implements CacheService {

    private final RedissonClient redissonClient;

    public FileRankCacheService(@Qualifier("fileRankRedissonClient") RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void initPDFCachePool(Integer capacity) { }
    @Override
    public void initIMGCachePool(Integer capacity) { }
    @Override
    public void initPdfImagesCachePool(Integer capacity) { }

    @Override
    public void initMediaConvertCachePool(Integer capacity) {

    }

    @Override
    public void putPDFCache(String key, String value) { }

    @Override
    public void putImgCache(String key, List<String> value) { }

    @Override
    public Map<String, String> getPDFCache() {
        return new HashMap<>();
    }

    @Override
    public String getPDFCache(String key) {
        return null;
    }

    @Override
    public Map<String, List<String>> getImgCache() {
        return new HashMap<>();
    }

    @Override
    public List<String> getImgCache(String key) {
        return null;
    }

    @Override
    public Integer getPdfImageCache(String key) {
        return null;
    }

    @Override
    public void putPdfImageCache(String pdfFilePath, int num) { }

    @Override
    public Map<String, String> getMediaConvertCache() {
        return new HashMap<>();
    }

    @Override
    public void putMediaConvertCache(String key, String value) { }

    @Override
    public String getMediaConvertCache(String key) {
        return null;
    }

    @Override
    public void cleanCache() { }

    @Override
    public void addQueueTask(String url) { }

    @Override
    public String takeQueueTask() throws InterruptedException {
        return null;
    }

    @Override
    public void incrementFilePreviewCount(String fileName) {
        if (redissonClient != null) {
            redissonClient.getScoredSortedSet(FILE_PREVIEW_COUNT_KEY).addScore(fileName, 1);
        }
    }

    @Override
    public long getFilePreviewCount(String fileName) {
        if (redissonClient != null) {
            Double score = redissonClient.getScoredSortedSet(FILE_PREVIEW_COUNT_KEY).getScore(fileName);
            return score != null ? score.longValue() : 0;
        }
        return 0;
    }

    @Override
    public List<Map<String, Object>> getFilePreviewRank(int topN) {
        List<Map<String, Object>> rankList = new ArrayList<>();
        if (redissonClient != null) {
            Collection<String> topFiles = redissonClient.getScoredSortedSet(FILE_PREVIEW_COUNT_KEY)
                    .descendingSet()
                    .readAll(0, topN - 1);
            
            int rank = 1;
            for (String fileName : topFiles) {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("rank", rank++);
                fileInfo.put("fileName", fileName);
                fileInfo.put("count", getFilePreviewCount(fileName));
                rankList.add(fileInfo);
            }
        }
        return rankList;
    }
}