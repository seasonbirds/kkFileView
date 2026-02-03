package cn.keking.service;

import java.util.List;
import java.util.Map;

/**
 * 文件预览排行榜服务接口
 * 用于统计和查询文件预览次数排行
 *
 * @author kl
 * @since 2024/01/01
 */
public interface FilePreviewRankService {

    String FILE_PREVIEW_RANK_KEY = "file-preview-rank";

    /**
     * 增加文件预览次数
     *
     * @param fileName 文件名称
     */
    void incrementPreviewCount(String fileName);

    /**
     * 获取文件预览排行榜
     *
     * @param topN 前N名
     * @return 排行榜列表，每个元素包含排序、文件名称、预览次数
     */
    List<Map<String, Object>> getTopN(int topN);

    /**
     * 获取文件预览次数
     *
     * @param fileName 文件名称
     * @return 预览次数
     */
    long getPreviewCount(String fileName);

    /**
     * 清除排行榜数据
     */
    void clearRank();
}
