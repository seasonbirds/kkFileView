package cn.keking.web.controller;

import cn.keking.service.cache.CacheService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 最受欢迎文件排行榜控制器
 */
@Controller
public class RankingController {

    private final CacheService cacheService;

    public RankingController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/ranking")
    public String ranking(Model model) {
        return "main/ranking";
    }

    @GetMapping("/api/ranking/topFiles")
    @ResponseBody
    public Map<String, Object> getTopFiles(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> topFiles = cacheService.getTopPreviewFiles(limit);
            result.put("code", 0);
            result.put("data", topFiles);
            result.put("msg", "success");
        } catch (Exception e) {
            result.put("code", -1);
            result.put("msg", "获取排行榜失败: " + e.getMessage());
        }
        return result;
    }
}
