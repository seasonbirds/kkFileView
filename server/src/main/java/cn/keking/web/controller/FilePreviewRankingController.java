package cn.keking.web.controller;

import cn.keking.service.FilePreviewStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件预览排行榜控制器
 *
 * @author kl
 * @date 2026/02/25
 */
@Controller
public class FilePreviewRankingController {

    private static final Logger logger = LoggerFactory.getLogger(FilePreviewRankingController.class);

    private final FilePreviewStatisticsService statisticsService;

    public FilePreviewRankingController(FilePreviewStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    /**
     * 跳转到排行榜页面
     *
     * @return 页面路径
     */
    @GetMapping("/ranking")
    public String go2Ranking() {
        return "/main/ranking";
    }

    /**
     * 获取文件预览排行榜数据（API接口）
     *
     * @param topN 前N名，默认为10
     * @return 排行榜数据
     */
    @GetMapping("/api/ranking")
    @ResponseBody
    public Map<String, Object> getRanking(@RequestParam(defaultValue = "10") int topN) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 限制最大查询数量
            if (topN > 100) {
                topN = 100;
            }
            if (topN < 1) {
                topN = 10;
            }

            List<Map<String, Object>> rankingList = statisticsService.getPreviewRanking(topN);
            result.put("code", 0);
            result.put("msg", "success");
            result.put("data", rankingList);
            result.put("total", rankingList.size());
        } catch (Exception e) {
            logger.error("获取排行榜数据失败", e);
            result.put("code", 500);
            result.put("msg", "获取排行榜数据失败: " + e.getMessage());
            result.put("data", null);
        }
        return result;
    }
}
