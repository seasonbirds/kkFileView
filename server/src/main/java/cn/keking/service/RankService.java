package cn.keking.service;

import cn.keking.config.RankRedisConfig;
import org.redisson.Redisson;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Service
public class RankService {

    private static final Logger logger = LoggerFactory.getLogger(RankService.class);
    private static final String RANK_KEY = "kkFileView:file:preview:rank";
    private static final String FILE_NAME_PREFIX = "kkFileView:file:name:";

    private final RankRedisConfig rankRedisConfig;
    private RedissonClient redissonClient;
    private boolean enabled = false;

    public RankService(RankRedisConfig rankRedisConfig) {
        this.rankRedisConfig = rankRedisConfig;
    }

    @PostConstruct
    public void init() {
        this.enabled = rankRedisConfig.isEnabled();
        if (enabled) {
            try {
                Config config = rankRedisConfig.rankRedisConfig();
                if (config != null) {
                    this.redissonClient = Redisson.create(config);
                    logger.info("Rank Redis client initialized successfully");
                }
            } catch (Exception e) {
                logger.error("Failed to initialize Rank Redis client: {}", e.getMessage());
                this.enabled = false;
            }
        }
    }

    @PreDestroy
    public void destroy() {
        if (redissonClient != null) {
            redissonClient.shutdown();
            logger.info("Rank Redis client shutdown");
        }
    }

    public boolean isEnabled() {
        return enabled && redissonClient != null;
    }

    public void incrementPreviewCount(String fileUrl, String fileName) {
        if (!isEnabled()) {
            return;
        }
        try {
            RScoredSortedSet<String> sortedSet = redissonClient.getScoredSortedSet(RANK_KEY);
            sortedSet.addScoreAsync(fileUrl, 1.0);
            if (fileName != null && !fileName.isEmpty()) {
                redissonClient.getBucket(FILE_NAME_PREFIX + fileUrl).setAsync(fileName);
            }
        } catch (Exception e) {
            logger.error("Failed to increment preview count for file: {}", fileUrl, e);
        }
    }

    public List<RankItem> getTopFiles(int topN) {
        List<RankItem> result = new ArrayList<>();
        if (!isEnabled()) {
            return result;
        }
        try {
            RScoredSortedSet<String> sortedSet = redissonClient.getScoredSortedSet(RANK_KEY);
            var entries = sortedSet.entryRangeReversed(0, topN - 1);
            int rank = 1;
            for (var entry : entries) {
                String fileUrl = entry.getValue();
                double score = entry.getScore();
                String fileName = getFileName(fileUrl);
                result.add(new RankItem(rank++, fileUrl, fileName, (long) score));
            }
        } catch (Exception e) {
            logger.error("Failed to get top files: {}", e.getMessage());
        }
        return result;
    }

    private String getFileName(String fileUrl) {
        if (!isEnabled()) {
            return extractFileNameFromUrl(fileUrl);
        }
        try {
            String fileName = (String) redissonClient.getBucket(FILE_NAME_PREFIX + fileUrl).get();
            if (fileName != null && !fileName.isEmpty()) {
                return fileName;
            }
        } catch (Exception e) {
            logger.error("Failed to get file name for url: {}", fileUrl, e);
        }
        return extractFileNameFromUrl(fileUrl);
    }

    private String extractFileNameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "unknown";
        }
        try {
            int lastSlash = fileUrl.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < fileUrl.length() - 1) {
                return fileUrl.substring(lastSlash + 1);
            }
        } catch (Exception e) {
            logger.error("Failed to extract file name from url: {}", fileUrl, e);
        }
        return fileUrl;
    }

    public static class RankItem {
        private int rank;
        private String fileUrl;
        private String fileName;
        private long previewCount;

        public RankItem(int rank, String fileUrl, String fileName, long previewCount) {
            this.rank = rank;
            this.fileUrl = fileUrl;
            this.fileName = fileName;
            this.previewCount = previewCount;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public long getPreviewCount() {
            return previewCount;
        }

        public void setPreviewCount(long previewCount) {
            this.previewCount = previewCount;
        }
    }
}
