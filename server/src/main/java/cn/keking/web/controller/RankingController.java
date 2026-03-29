package cn.keking.web.controller;

import cn.keking.model.ReturnResponse;
import cn.keking.service.PreviewCountService;
import cn.keking.service.PreviewCountService.FilePreviewCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 文件预览排名控制器
 * 
 * @author Trae AI
 * @since 2024/03/29
 */
@Controller
public class RankingController {

    private final Logger logger = LoggerFactory.getLogger(RankingController.class);

    @Autowired
    private PreviewCountService previewCountService;

    /**
     * 访问排名页面
     */
    @GetMapping("/ranking")
    public String go2Ranking(Model model) {
        // 默认显示Top10
        List<FilePreviewCount> topFiles = previewCountService.getTopPreviewFiles(10);
        model.addAttribute("topFiles", topFiles);
        model.addAttribute("currentTop", 10);
        return "/main/ranking";
    }

    /**
     * 获取Top N预览文件API
     */
    @GetMapping("/api/ranking")
    @ResponseBody
    public ReturnResponse<Object> getRanking(
            @RequestParam(defaultValue = "10") int topN) {
        
        try {
            if (topN < 1 || topN > 100) {
                topN = 10; // 默认值
            }
            List<FilePreviewCount> topFiles = previewCountService.getTopPreviewFiles(topN);
            return ReturnResponse.success(topFiles);
        } catch (Exception e) {
            logger.error("获取文件预览排名失败", e);
            return ReturnResponse.failure("获取排名失败");
        }
    }
}
