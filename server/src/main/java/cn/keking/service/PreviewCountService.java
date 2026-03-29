package cn.keking.service;

import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件预览次数统计服务，基于独立Redis配置实现
 * 不依赖于原有cache.type配置，确保排名功能始终可用
 */
@Service
public class PreviewCountService {

    private static final Logger logger = LoggerFactory.getLogger(PreviewCountService.class);

    private static final String PREVIEW_COUNT_KEY = "kkfileview:preview:count";
    private static final String SEPARATOR = "|||";

    private final RedissonClient redissonClient;

    @Autowired
    public PreviewCountService(@Qualifier("rankingRedissonClient") RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 增加文件预览次数
     *
     * @param fileUrl 文件URL
     */
    public void incrementPreviewCount(String fileUrl) {
        try {
            RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(PREVIEW_COUNT_KEY);
            scoredSet.addScoreAsync(fileUrl, 1);
        } catch (Exception e) {
            logger.error("增加文件预览次数失败, fileUrl: {}", fileUrl, e);
        }
    }
    
    /**
     * 增加文件预览次数
     *
     * @param fileUrl 文件URL
     * @param fileName 文件名称
     */
    public void incrementPreviewCount(String fileUrl, String fileName) {
        try {
            RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(PREVIEW_COUNT_KEY);
            String combinedKey = fileUrl + SEPARATOR + fileName;
            scoredSet.addScoreAsync(combinedKey, 1);
        } catch (Exception e) {
            logger.error("增加文件预览次数失败, fileUrl: {}, fileName: {}", fileUrl, fileName, e);
        }
    }

    /**
     * 获取Top N最受欢迎的文件
     *
     * @param topN 取前N个
     * @return 文件列表
     */
    public List<FilePreviewCount> getTopPreviewFiles(int topN) {
        RScoredSortedSet<String> scoredSet = redissonClient.getScoredSortedSet(PREVIEW_COUNT_KEY);
        // 倒序获取Top N
        return scoredSet.entryRangeReversed(0, topN - 1)
                .stream()
                .map(entry -> {
                    String[] parts = entry.getValue().split(SEPARATOR, 2);
                    String fileUrl = parts[0];
                    String fileName = parts.length > 1 ? parts[1] : fileUrl; // Fallback to fileUrl if no fileName found
                    return new FilePreviewCount(fileUrl, fileName, entry.getScore().intValue());
                })
                .collect(Collectors.toList());
    }

    /**
     * 文件预览次数模型
     */
    public static class FilePreviewCount {
        private String fileUrl;
        private String fileName;
        private int count;

        public FilePreviewCount() {
        }

        public FilePreviewCount(String fileUrl, int count) {
            this.fileUrl = fileUrl;
            this.fileName = fileUrl;
            this.count = count;
        }
        
        public FilePreviewCount(String fileUrl, String fileName, int count) {
            this.fileUrl = fileUrl;
            this.fileName = fileName;
            this.count = count;
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

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}
