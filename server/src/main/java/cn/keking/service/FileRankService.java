package cn.keking.service;

import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

@ConditionalOnBean(name = "fileRankRedissonClient")
@Service
public class FileRankService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileRankService.class);
    
    private static final String FILE_RANK_KEY = "kkFileView:fileRank";
    
    private final RedissonClient redissonClient;

    public FileRankService(@Qualifier("fileRankRedissonClient") RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void incrementPreviewCount(String url) {
        try {
            if (url == null || url.isEmpty()) {
                return;
            }
            String normalizedUrl = normalizeUrl(url);
            RScoredSortedSet<String> rankedSet = redissonClient.getScoredSortedSet(FILE_RANK_KEY);
            rankedSet.addScore(normalizedUrl, 1);
        } catch (Exception e) {
            LOGGER.error("Failed to increment preview count for url: {}", url, e);
        }
    }

    public List<FileRankItem> getTopFiles(int topN) {
        List<FileRankItem> result = new ArrayList<>();
        try {
            RScoredSortedSet<String> rankedSet = redissonClient.getScoredSortedSet(FILE_RANK_KEY);
            rankedSet.valueRangeReversed(0, topN - 1).forEach(url -> {
                Double score = rankedSet.getScore(url);
                String fileName = extractFileName(url);
                result.add(new FileRankItem(url, fileName, score != null ? score.intValue() : 0));
            });
        } catch (Exception e) {
            LOGGER.error("Failed to get top {} files", topN, e);
        }
        return result;
    }

    private String normalizeUrl(String url) {
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return url;
        }
    }

    private String extractFileName(String url) {
        if (url == null || url.isEmpty()) {
            return "unknown";
        }
        try {
            String decodedUrl = URLDecoder.decode(url, "UTF-8");
            String cleanUrl = decodedUrl;
            int queryIndex = cleanUrl.indexOf("?");
            if (queryIndex > 0) {
                cleanUrl = cleanUrl.substring(0, queryIndex);
            }
            cleanUrl = cleanUrl.replaceAll("\\\\", "/");
            int lastSlash = cleanUrl.lastIndexOf("/");
            if (lastSlash >= 0 && lastSlash < cleanUrl.length() - 1) {
                return cleanUrl.substring(lastSlash + 1);
            }
            return cleanUrl;
        } catch (Exception e) {
            return url.length() > 50 ? url.substring(0, 50) + "..." : url;
        }
    }

    public static class FileRankItem {
        private String url;
        private String fileName;
        private int previewCount;
        private int rank;

        public FileRankItem(String url, String fileName, int previewCount) {
            this.url = url;
            this.fileName = fileName;
            this.previewCount = previewCount;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
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

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }
    }
}