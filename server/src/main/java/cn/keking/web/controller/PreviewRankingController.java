package cn.keking.web.controller;

import cn.keking.service.PreviewCountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件预览排行榜控制器
 * @author kkfileview
 */
@Controller
@RequestMapping("/ranking")
public class PreviewRankingController {
    
    private static final Logger logger = LoggerFactory.getLogger(PreviewRankingController.class);
    
    private final PreviewCountService previewCountService;
    
    public PreviewRankingController(PreviewCountService previewCountService) {
        this.previewCountService = previewCountService;
    }
    
    /**
     * 获取文件预览排行榜页面
     */
    @GetMapping
    public String rankingPage() {
        return "ranking";
    }
    
    /**
     * 获取文件预览排行榜数据
     * @param topN 前N个文件，默认10
     */
    @GetMapping("/data")
    @ResponseBody
    public Map<String, Object> getRankingData(@RequestParam(defaultValue = "10") int topN) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 限制topN的范围
            if (topN <= 0) {
                topN = 10;
            } else if (topN > 100) {
                topN = 100;
            }
            
            List<Map<String, Object>> topFiles = previewCountService.getTopFiles(topN);
            
            result.put("success", true);
            result.put("data", topFiles);
            result.put("total", topFiles.size());
            result.put("topN", topN);
            
            logger.info("获取文件预览排行榜数据成功，topN: {}", topN);
        } catch (Exception e) {
            logger.error("获取文件预览排行榜数据失败", e);
            result.put("success", false);
            result.put("message", "获取排行榜数据失败: " + e.getMessage());
        }
        
        return result;
    }
}