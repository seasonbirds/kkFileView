package cn.keking.service;

import java.util.List;

/**
 * 预览排行榜服务接口
 * 提供文件预览次数统计和排行榜查询功能
 */
public interface PreviewRankService {

    /**
     * Redis中存储排行榜数据的Key
     * 使用Sorted Set结构，score为预览次数，member为文件名
     */
    String PREVIEW_RANK_KEY = "file-preview-rank";

    /**
     * 增加文件的预览次数
     * 使用Redis ZINCRBY原子操作，保证高并发下的计数准确性
     *
     * @param fileName 文件名称
     * @param fileUrl  文件URL地址
     */
    void incrementPreviewCount(String fileName, String fileUrl);

    /**
     * 获取预览次数排行榜
     * 按预览次数降序排列，返回前N名
     *
     * @param topN 返回的记录数量
     * @return 排行榜列表
     */
    List<PreviewRankItem> getTopRankList(int topN);

    /**
     * 排行榜条目实体类
     */
    class PreviewRankItem {
        private int rank;
        private String fileName;
        private long previewCount;
        private String fileUrl;

        public PreviewRankItem() {
        }

        public PreviewRankItem(int rank, String fileName, long previewCount, String fileUrl) {
            this.rank = rank;
            this.fileName = fileName;
            this.previewCount = previewCount;
            this.fileUrl = fileUrl;
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

        public String getFileUrl() {
            return fileUrl;
        }

        public void setFileUrl(String fileUrl) {
            this.fileUrl = fileUrl;
        }
    }
}
