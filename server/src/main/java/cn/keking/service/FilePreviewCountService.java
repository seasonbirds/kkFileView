package cn.keking.service;

import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilePreviewCountService {

    private static final Logger logger = LoggerFactory.getLogger(FilePreviewCountService.class);
    
    private static final String STATISTICS_REDIS_KEY = "file:preview:statistics";
    
    private final RedissonClient statisticsRedissonClient;

    public FilePreviewCountService(@Qualifier("statisticsRedissonClient") RedissonClient statisticsRedissonClient) {
        this.statisticsRedissonClient = statisticsRedissonClient;
    }

    public void incrementPreviewCount(String fileName) {
        try {
            RScoredSortedSet<String> scoredSortedSet = statisticsRedissonClient.getScoredSortedSet(STATISTICS_REDIS_KEY);
            scoredSortedSet.addScore(fileName, 1.0);
        } catch (Exception e) {
            logger.error("Increment preview count error for file: {}", fileName, e);
        }
    }

    public List<FileRankingItem> getTopFiles(int topN) {
        try {
            RScoredSortedSet<String> scoredSortedSet = statisticsRedissonClient.getScoredSortedSet(STATISTICS_REDIS_KEY);
            return scoredSortedSet.entryRangeReversed(0, topN - 1).stream()
                    .map(entry -> new FileRankingItem(
                            entry.getValue(),
                            entry.getScore().intValue()
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Get top files error", e);
            return List.of();
        }
    }

    public static class FileRankingItem {
        private String fileName;
        private int previewCount;

        public FileRankingItem(String fileName, int previewCount) {
            this.fileName = fileName;
            this.previewCount = previewCount;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public int getPreviewCount() {
            return previewCount;
        }

        public void setPreviewCount(int previewCount) {
            this.previewCount = previewCount;
        }
    }
}
