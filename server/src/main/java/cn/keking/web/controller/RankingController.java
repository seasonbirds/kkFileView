package cn.keking.web.controller;

import cn.keking.service.FilePreviewCounterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * 排行榜控制器，提供获取热门文件列表的API接口
 */
@Controller
public class RankingController {

    private static final Logger logger = LoggerFactory.getLogger(RankingController.class);

    @Autowired
    private FilePreviewCounterService counterService;

    /**
     * 排行榜页面路由
     */
    @GetMapping("/ranking")
    public String rankingPage(Model model) {
        // 默认展示Top10
        model.addAttribute("topSize", 10);
        return "ranking";
    }

    /**
     * 获取热门文件列表API
     * @param topSize 前N名，默认10，支持50、100
     * @return 热门文件列表，包含文件名和预览次数
     */
    @GetMapping("/api/ranking/topFiles")
    @ResponseBody
    public List<Map<String, Object>> getTopFiles(@RequestParam(defaultValue = "10") int topSize) {
        // 限制topSize的取值范围
        if (topSize <= 0 || topSize > 100) {
            topSize = 10;
        }
        logger.info("获取热门文件列表，topSize：{}", topSize);
        return counterService.getTopFiles(topSize);
    }

    /**
     * 获取单个文件的预览次数
     * @param fileName 文件名
     * @return 预览次数
     */
    @GetMapping("/api/ranking/count")
    @ResponseBody
    public Long getPreviewCount(@RequestParam String fileName) {
        return counterService.getPreviewCount(fileName);
    }
}
