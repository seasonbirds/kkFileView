package cn.keking.service;

import java.util.List;
import java.util.Map;

/**
 * 文件预览次数统计服务
 * @author kkfileview
 */
public interface PreviewCountService {
    
    /**
     * 增加文件预览次数
     * @param fileUrl 文件URL
     */
    void incrementPreviewCount(String fileUrl);
    
    /**
     * 获取文件预览次数
     * @param fileUrl 文件URL
     * @return 预览次数
     */
    long getPreviewCount(String fileUrl);
    
    /**
     * 获取最受欢迎文件排行榜
     * @param topN 前N名
     * @return 排行榜列表，包含文件URL和预览次数
     */
    List<Map<String, Object>> getTopFiles(int topN);
    
    /**
     * 获取文件名（从URL中提取）
     * @param fileUrl 文件URL
     * @return 文件名
     */
    String getFileNameFromUrl(String fileUrl);
}