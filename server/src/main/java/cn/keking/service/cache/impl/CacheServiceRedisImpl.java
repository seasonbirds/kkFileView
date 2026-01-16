package cn.keking.service.cache.impl;

import cn.keking.service.cache.CacheService;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RMapCache;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @auther: chenjh
 * @time: 2019/4/2 18:02
 * @description
 */
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
        RMapCache<String, String> convertedList = redissonClient.getMapCache(FILE_PREVIEW_PDF_KEY);
        convertedList.fastPut(key, value);
    }

    @Override
    public void putImgCache(String key, List<String> value) {
        RMapCache<String, List<String>> convertedList = redissonClient.getMapCache(FILE_PREVIEW_IMGS_KEY);
        convertedList.fastPut(key, value);
    }

    @Override
    public Map<String, String> getPDFCache() {
        return redissonClient.getMapCache(FILE_PREVIEW_PDF_KEY);
    }

    @Override
    public String getPDFCache(String key) {
        RMapCache<String, String> convertedList = redissonClient.getMapCache(FILE_PREVIEW_PDF_KEY);
        return convertedList.get(key);
    }

    @Override
    public Map<String, List<String>> getImgCache() {
        return redissonClient.getMapCache(FILE_PREVIEW_IMGS_KEY);
    }

    @Override
    public List<String> getImgCache(String key) {
        RMapCache<String, List<String>> convertedList = redissonClient.getMapCache(FILE_PREVIEW_IMGS_KEY);
        return convertedList.get(key);
    }

    @Override
    public Integer getPdfImageCache(String key) {
        RMapCache<String, Integer> convertedList = redissonClient.getMapCache(FILE_PREVIEW_PDF_IMGS_KEY);
        return convertedList.get(key);
    }

    @Override
    public void putPdfImageCache(String pdfFilePath, int num) {
        RMapCache<String, Integer> convertedList = redissonClient.getMapCache(FILE_PREVIEW_PDF_IMGS_KEY);
        convertedList.fastPut(pdfFilePath, num);
    }

    @Override
    public Map<String, String> getMediaConvertCache() {
        return redissonClient.getMapCache(FILE_PREVIEW_MEDIA_CONVERT_KEY);
    }

    @Override
    public void putMediaConvertCache(String key, String value) {
        RMapCache<String, String> convertedList = redissonClient.getMapCache(FILE_PREVIEW_MEDIA_CONVERT_KEY);
        convertedList.fastPut(key, value);
    }

    @Override
    public String getMediaConvertCache(String key) {
        RMapCache<String, String> convertedList = redissonClient.getMapCache(FILE_PREVIEW_MEDIA_CONVERT_KEY);
        return convertedList.get(key);
    }

    @Override
    public void cleanCache() {
        cleanPdfCache();
        cleanImgCache();
        cleanPdfImgCache();
        cleanMediaConvertCache();
    }

    @Override
    public void addQueueTask(String url) {
        RBlockingQueue<String> queue = redissonClient.getBlockingQueue(TASK_QUEUE_NAME);
        queue.addAsync(url);
    }

    @Override
    public String takeQueueTask() throws InterruptedException {
        RBlockingQueue<String> queue = redissonClient.getBlockingQueue(TASK_QUEUE_NAME);
        return queue.take();
    }

    @Override
    public void incrementPreviewCount(String fileName) {
        RScoredSortedSet<String> rankingSet = redissonClient.getScoredSortedSet(FILE_PREVIEW_RANKING_KEY);
        rankingSet.addScoreAsync(fileName, 1);
    }

    @Override
    public List<Map<String, Object>> getTopPreviewFiles(int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        RScoredSortedSet<String> rankingSet = redissonClient.getScoredSortedSet(FILE_PREVIEW_RANKING_KEY);
        
        List<String> topFiles = rankingSet.valueRangeReversed(0, limit - 1);
        
        for (int i = 0; i < topFiles.size(); i++) {
            String fileName = topFiles.get(i);
            Double score = rankingSet.getScore(fileName);
            
            Map<String, Object> fileInfo = new HashMap<>();
            fileInfo.put("rank", i + 1);
            fileInfo.put("fileName", fileName);
            fileInfo.put("previewCount", score.longValue());
            
            result.add(fileInfo);
        }
        
        return result;
    }

    private void cleanPdfCache() {
        RMapCache<String, String> pdfCache = redissonClient.getMapCache(FILE_PREVIEW_PDF_KEY);
        pdfCache.clear();
    }

    private void cleanImgCache() {
        RMapCache<String, List<String>> imgCache = redissonClient.getMapCache(FILE_PREVIEW_IMGS_KEY);
        imgCache.clear();
    }

    private void cleanPdfImgCache() {
        RMapCache<String, Integer> pdfImg = redissonClient.getMapCache(FILE_PREVIEW_PDF_IMGS_KEY);
        pdfImg.clear();
    }

    private void cleanMediaConvertCache() {
        RMapCache<String, Integer> mediaConvertCache = redissonClient.getMapCache(FILE_PREVIEW_MEDIA_CONVERT_KEY);
        mediaConvertCache.clear();
    }
}
