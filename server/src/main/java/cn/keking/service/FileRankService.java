package cn.keking.service;

import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnBean(name = "rankRedissonClient")
public class FileRankService {

    private static final Logger logger = LoggerFactory.getLogger(FileRankService.class);

    private static final String FILE_PREVIEW_RANK_KEY = "kkfileview:file:preview:rank";

    private final RedissonClient rankRedissonClient;

    public FileRankService(@Qualifier("rankRedissonClient") RedissonClient rankRedissonClient) {
        this.rankRedissonClient = rankRedissonClient;
    }

    public void incrementPreviewCount(String fileUrl, String fileName) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        try {
            RScoredSortedSet<String> rankedSet = rankRedissonClient.getScoredSortedSet(FILE_PREVIEW_RANK_KEY);
            String rankKey = buildRankKey(fileUrl, fileName);
            rankedSet.addScore(rankKey, 1.0);
        } catch (Exception e) {
            logger.warn("Failed to increment preview count for: {}", fileUrl, e);
        }
    }

    private String buildRankKey(String fileUrl, String fileName) {
        String safeFileName = fileName;
        if (safeFileName == null || safeFileName.isEmpty()) {
            safeFileName = extractFileNameFromUrl(fileUrl);
        }
        return fileUrl + "|||" + safeFileName;
    }

    private String extractFileNameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "unknown";
        }
        try {
            String decoded = java.net.URLDecoder.decode(fileUrl, "UTF-8");
            int lastSlash = decoded.lastIndexOf('/');
            int lastBackslash = decoded.lastIndexOf('\\');
            int pos = Math.max(lastSlash, lastBackslash);
            if (pos >= 0 && pos < decoded.length() - 1) {
                return decoded.substring(pos + 1);
            }
            return decoded;
        } catch (Exception e) {
            int lastSlash = fileUrl.lastIndexOf('/');
            int lastBackslash = fileUrl.lastIndexOf('\\');
            int pos = Math.max(lastSlash, lastBackslash);
            if (pos >= 0 && pos < fileUrl.length() - 1) {
                return fileUrl.substring(pos + 1);
            }
            return fileUrl;
        }
    }

    public List<FileRankItem> getTopFiles(int topN) {
        List<FileRankItem> result = new ArrayList<>();
        try {
            RScoredSortedSet<String> rankedSet = rankRedissonClient.getScoredSortedSet(FILE_PREVIEW_RANK_KEY);
            int totalSize = rankedSet.size();
            if (totalSize == 0) {
                return result;
            }
            int count = Math.min(topN, totalSize);
            var entries = rankedSet.entryRangeReversed(0, count - 1);
            int rank = 1;
            for (var entry : entries) {
                String rankKey = entry.getValue();
                String[] parts = parseRankKey(rankKey);
                result.add(new FileRankItem(rank++, parts[1], parts[0], entry.getScore().intValue()));
            }
        } catch (Exception e) {
            logger.warn("Failed to get top {} files", topN, e);
        }
        return result;
    }

    private String[] parseRankKey(String rankKey) {
        String[] result = new String[2];
        int separatorIndex = rankKey.lastIndexOf("|||");
        if (separatorIndex > 0) {
            result[0] = rankKey.substring(0, separatorIndex);
            result[1] = rankKey.substring(separatorIndex + 3);
        } else {
            result[0] = rankKey;
            result[1] = extractFileNameFromUrl(rankKey);
        }
        return result;
    }

    public static class FileRankItem {
        private int rank;
        private String fileName;
        private String fileUrl;
        private int previewCount;

        public FileRankItem(int rank, String fileName, String fileUrl, int previewCount) {
            this.rank = rank;
            this.fileName = fileName;
            this.fileUrl = fileUrl;
            this.previewCount = previewCount;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }

        public int getPreviewCount() {
            return previewCount;
        }

        public void setPreviewCount(int previewCount) {
            this.previewCount = previewCount;
        }
    }
}
