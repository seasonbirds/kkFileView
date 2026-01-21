package cn.keking.service.cache.impl;

import cn.keking.service.cache.CacheService;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RMapCache;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @auther: chenjh
 * @time: 2019/4/2 18:02
 * @description
 */
@ConditionalOnExpression("'${cache.type:default}'.equals('redis')")
@Service
public class CacheServiceRedisImpl implements CacheService {

    private final RedissonClient redissonClient;

    public CacheServiceRedisImpl(RedissonClient redissonClient) {
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
    public void putPDFCache(String key, String value) {
        if (redissonClient != null) {
            RMapCache<String, String> convertedList = redissonClient.getMapCache(FILE_PREVIEW_PDF_KEY);
            convertedList.fastPut(key, value);
        }
    }

    @Override
    public void putImgCache(String key, List<String> value) {
        if (redissonClient != null) {
            RMapCache<String, List<String>> convertedList = redissonClient.getMapCache(FILE_PREVIEW_IMGS_KEY);
            convertedList.fastPut(key, value);
        }
    }

    @Override
    public Map<String, String> getPDFCache() {
        if (redissonClient != null) {
            return redissonClient.getMapCache(FILE_PREVIEW_PDF_KEY);
        }
        return new HashMap<>();
    }

    @Override
    public String getPDFCache(String key) {
        if (redissonClient != null) {
            RMapCache<String, String> convertedList = redissonClient.getMapCache(FILE_PREVIEW_PDF_KEY);
            return convertedList.get(key);
        }
        return null;
    }

    @Override
    public Map<String, List<String>> getImgCache() {
        if (redissonClient != null) {
            return redissonClient.getMapCache(FILE_PREVIEW_IMGS_KEY);
        }
        return new HashMap<>();
    }

    @Override
    public List<String> getImgCache(String key) {
        if (redissonClient != null) {
            RMapCache<String, List<String>> convertedList = redissonClient.getMapCache(FILE_PREVIEW_IMGS_KEY);
            return convertedList.get(key);
        }
        return null;
    }

    @Override
    public Integer getPdfImageCache(String key) {
        if (redissonClient != null) {
            RMapCache<String, Integer> convertedList = redissonClient.getMapCache(FILE_PREVIEW_PDF_IMGS_KEY);
            return convertedList.get(key);
        }
        return null;
    }

    @Override
    public void putPdfImageCache(String pdfFilePath, int num) {
        if (redissonClient != null) {
            RMapCache<String, Integer> convertedList = redissonClient.getMapCache(FILE_PREVIEW_PDF_IMGS_KEY);
            convertedList.fastPut(pdfFilePath, num);
        }
    }

    @Override
    public Map<String, String> getMediaConvertCache() {
        if (redissonClient != null) {
            return redissonClient.getMapCache(FILE_PREVIEW_MEDIA_CONVERT_KEY);
        }
        return new HashMap<>();
    }

    @Override
    public void putMediaConvertCache(String key, String value) {
        if (redissonClient != null) {
            RMapCache<String, String> convertedList = redissonClient.getMapCache(FILE_PREVIEW_MEDIA_CONVERT_KEY);
            convertedList.fastPut(key, value);
        }
    }

    @Override
    public String getMediaConvertCache(String key) {
        if (redissonClient != null) {
            RMapCache<String, String> convertedList = redissonClient.getMapCache(FILE_PREVIEW_MEDIA_CONVERT_KEY);
            return convertedList.get(key);
        }
        return null;
    }

    @Override
    public void cleanCache() {
        if (redissonClient != null) {
            cleanPdfCache();
            cleanImgCache();
            cleanPdfImgCache();
            cleanMediaConvertCache();
        }
    }

    @Override
    public void addQueueTask(String url) {
        if (redissonClient != null) {
            redissonClient.getBlockingQueue(TASK_QUEUE_NAME).addAsync(url);
        }
    }

    @Override
    public String takeQueueTask() throws InterruptedException {
        if (redissonClient != null) {
            return redissonClient.getBlockingQueue(TASK_QUEUE_NAME).take();
        }
        return null;
    }

    private void cleanPdfCache() {
        if (redissonClient != null) {
            redissonClient.getMapCache(FILE_PREVIEW_PDF_KEY).clear();
        }
    }

    private void cleanImgCache() {
        if (redissonClient != null) {
            redissonClient.getMapCache(FILE_PREVIEW_IMGS_KEY).clear();
        }
    }

    private void cleanPdfImgCache() {
        if (redissonClient != null) {
            redissonClient.getMapCache(FILE_PREVIEW_PDF_IMGS_KEY).clear();
        }
    }

    private void cleanMediaConvertCache() {
        if (redissonClient != null) {
            redissonClient.getMapCache(FILE_PREVIEW_MEDIA_CONVERT_KEY).clear();
        }
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
