package cn.keking.service;

import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件预览排行榜服务
 * 使用Redis ZSet实现，保证高并发下的统计准确性
 */
@Service
public class FileRankService {

    private static final String FILE_PREVIEW_RANK_KEY = "kkfileview:file:preview:rank";

    private final RedissonClient redissonClient;

    public FileRankService(@Qualifier("rankRedissonClient") RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * 增加文件预览次数
     * 使用ZSet的原子操作，保证高并发下的准确性
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
     * 获取TopN文件预览排行榜
     *
     * @param topN 前N名
     * @return 排行榜列表
     */
    public List<FileRankVO> getTopFiles(int topN) {
        List<FileRankVO> result = new ArrayList<>();
        RScoredSortedSet<String> rankedSet = redissonClient.getScoredSortedSet(FILE_PREVIEW_RANK_KEY);
        
        // 从ZSet中按分数从高到低获取前N个元素
        rankedSet.entryRangeReversed(0, topN - 1).forEach(entry -> {
            FileRankVO vo = new FileRankVO();
            vo.setFileName(entry.getValue());
            vo.setPreviewCount((int) Math.round(entry.getScore()));
            result.add(vo);
        });
        
        // 设置排名
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }
        
        return result;
    }

    /**
     * 文件排行榜VO
     */
    public static class FileRankVO {
        private int rank;
        private String fileName;
        private int previewCount;

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

        public int getPreviewCount() {
            return previewCount;
        }

        public void setPreviewCount(int previewCount) {
            this.previewCount = previewCount;
        }
    }
}
