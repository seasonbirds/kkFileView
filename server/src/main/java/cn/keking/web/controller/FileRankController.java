package cn.keking.web.controller;

import cn.keking.service.cache.CacheService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

/**
 * 文件排行榜控制器
 */
@Controller
public class FileRankController {

    private final CacheService fileRankCacheService;

    public FileRankController(@Qualifier("fileRankCacheService") CacheService fileRankCacheService) {
        this.fileRankCacheService = fileRankCacheService;
    }

    @GetMapping("/fileRank")
    public String fileRank(Integer topN, Model model) {
        // 默认展示top10
        if (topN == null || topN <= 0) {
            topN = 10;
        }
        // 限制最大展示数量为100
        if (topN > 100) {
            topN = 100;
        }
        // 获取文件预览排行
        List<Map<String, Object>> rankList = fileRankCacheService.getFilePreviewRank(topN);
        model.addAttribute("rankList", rankList);
        model.addAttribute("currentTopN", topN);
        return "fileRank";
    }
}
