package cn.keking.service;

import java.util.List;
import java.util.Map;

/**
 * 文件预览统计服务接口
 * 用于统计文件预览次数，生成排行榜
 *
 * @author kl
 * @date 2026/02/25
 */
public interface FilePreviewStatisticsService {

    String FILE_PREVIEW_COUNT_KEY = "kkfileview:preview:count";
    String FILE_PREVIEW_RANKING_KEY = "kkfileview:preview:ranking";

    /**
     * 增加文件预览次数
     *
     * @param fileName 文件名
     * @param fileUrl  文件URL
     */
    void incrementPreviewCount(String fileName, String fileUrl);

    /**
     * 获取文件预览排行榜
     *
     * @param topN 前N名
     * @return 排行榜列表，包含文件名和预览次数
     */
    List<Map<String, Object>> getPreviewRanking(int topN);

    /**
     * 获取文件预览次数
     *
     * @param fileUrl 文件URL
     * @return 预览次数
     */
    Long getPreviewCount(String fileUrl);
}
