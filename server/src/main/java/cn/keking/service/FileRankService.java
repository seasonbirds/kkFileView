package cn.keking.service;

import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件预览排行榜服务
 * 使用Redis Sorted Set实现高并发下的原子计数和排序
 */
@Service
public class FileRankService {

    private static final String FILE_PREVIEW_RANK_KEY = "file:preview:rank";

    private final RedissonClient redissonClient;

    @Autowired
    public FileRankService(@Qualifier("fileRankRedissonClient") RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 增加文件预览次数
     * 使用Redis Sorted Set的ZINCRBY原子操作，确保高并发下计数准确
     *
     * @param fileName 文件名
     */
    public void incrementPreviewCount(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return;
        }
        RScoredSortedSet<String> rankedSet = redissonClient.getScoredSortedSet(FILE_PREVIEW_RANK_KEY);
        rankedSet.addScore(fileName, 1);
    }

    /**
     * 获取TopN排行榜
     *
     * @param topN 前N名
     * @return 排行榜列表
     */
    public List<FileRankVO> getTopFiles(int topN) {
        RScoredSortedSet<String> rankedSet = redissonClient.getScoredSortedSet(FILE_PREVIEW_RANK_KEY);
        // 按分数从高到低获取前N个元素 (REVERSED ORDER)
        List<String> topFiles = new ArrayList<>(rankedSet.valueRangeReversed(0, topN - 1));
        List<FileRankVO> result = new ArrayList<>(topFiles.size());

        int rank = 1;
        for (String fileName : topFiles) {
            Double score = rankedSet.getScore(fileName);
            result.add(new FileRankVO(rank++, fileName, score != null ? score.longValue() : 0));
        }
        return result;
    }

    /**
     * 获取单个文件的预览次数
     *
     * @param fileName 文件名
     * @return 预览次数
     */
    public long getPreviewCount(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return 0;
        }
        RScoredSortedSet<String> rankedSet = redissonClient.getScoredSortedSet(FILE_PREVIEW_RANK_KEY);
        Double score = rankedSet.getScore(fileName);
        return score != null ? score.longValue() : 0;
    }

    /**
     * 文件排行榜VO
     */
    public static class FileRankVO {
        private int rank;
        private String fileName;
        private long previewCount;

        public FileRankVO() {
        }

        public FileRankVO(int rank, String fileName, long previewCount) {
            this.rank = rank;
            this.fileName = fileName;
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

        public long getPreviewCount() {
            return previewCount;
        }

        public void setPreviewCount(long previewCount) {
            this.previewCount = previewCount;
        }
    }
}
