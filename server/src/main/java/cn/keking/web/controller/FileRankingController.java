package cn.keking.web.controller;

import cn.keking.service.FilePreviewCountService;
import cn.keking.service.FilePreviewCountService.FileRankingItem;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 文件预览排行榜控制器
 */
@Controller
public class FileRankingController {

    private final FilePreviewCountService filePreviewCountService;

    public FileRankingController(FilePreviewCountService filePreviewCountService) {
        this.filePreviewCountService = filePreviewCountService;
    }

    /**
     * 获取排行榜页面
     */
    @GetMapping("/ranking")
    public String ranking(@RequestParam(defaultValue = "10") int topN, Model model) {
        if (topN != 10 && topN != 50 && topN != 100) {
            topN = 10;
        }
        List<FileRankingItem> rankingList = filePreviewCountService.getTopFiles(topN);
        model.addAttribute("rankingList", rankingList);
        model.addAttribute("topN", topN);
        return "/main/ranking";
    }
}
