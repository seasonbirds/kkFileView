package cn.keking.web.controller;

import cn.keking.model.ReturnResponse;
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
public class FileRankingController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileRankingController.class);
    
    private final PreviewCountService previewCountService;
    
    public FileRankingController(PreviewCountService previewCountService) {
        this.previewCountService = previewCountService;
    }
    
    /**
     * 排行榜页面
     */
    @GetMapping("/ranking")
    public String ranking(Model model) {
        // 默认获取Top10
        List<Map<String, Object>> topFiles = previewCountService.getTopFiles(10);
        model.addAttribute("topFiles", topFiles);
        model.addAttribute("topN", 10);
        return "/main/ranking";
    }
    
    /**
     * 获取排行榜数据API
     * @param topN 前N名，默认10
     * @return 排行榜数据
     */
    @GetMapping("/api/ranking")
    @ResponseBody
    public ReturnResponse<Object> getRanking(@RequestParam(defaultValue = "10") int topN) {
        try {
            // 限制最大查询数量为100，防止性能问题
            if (topN > 100) {
                topN = 100;
            }
            
            List<Map<String, Object>> topFiles = previewCountService.getTopFiles(topN);
            
            Map<String, Object> result = new HashMap<>();
            result.put("topFiles", topFiles);
            result.put("topN", topN);
            
            return ReturnResponse.success(result);
        } catch (Exception e) {
            logger.error("获取排行榜数据失败", e);
            return ReturnResponse.failure("获取排行榜数据失败");
        }
    }
}