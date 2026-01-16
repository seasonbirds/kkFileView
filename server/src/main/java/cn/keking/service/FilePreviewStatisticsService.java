package cn.keking.service;

import java.util.List;
import java.util.Map;

public interface FilePreviewStatisticsService {

    String PREVIEW_COUNT_KEY = "file:preview:count";

    void incrementPreviewCount(String fileName);

    List<Map<String, Object>> getTopPreviewFiles(int limit);

    void clearStatistics();
}
