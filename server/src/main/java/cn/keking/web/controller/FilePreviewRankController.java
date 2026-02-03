package cn.keking.web.controller;

import cn.keking.service.FilePreviewRankService;
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
 * @since 2024/01/01
 */
@Controller
public class FilePreviewRankController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePreviewRankController.class);

    private final FilePreviewRankService filePreviewRankService;

    public FilePreviewRankController(FilePreviewRankService filePreviewRankService) {
        this.filePreviewRankService = filePreviewRankService;
    }

    /**
     * 排行榜页面
     *
     * @param model Model
     * @return 页面路径
     */
    @GetMapping("/previewRank")
    public String previewRankPage(Model model) {
        LOGGER.info("访问文件预览排行榜页面");
        return "main/previewRank";
    }

    /**
     * 获取文件预览排行榜数据
     *
     * @param topN 前N名，默认10
     * @return 排行榜数据
     */
    @GetMapping("/api/previewRank/topN")
    @ResponseBody
    public Map<String, Object> getTopN(@RequestParam(defaultValue = "10") int topN) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 限制最大值为100
            if (topN > 100) {
                topN = 100;
            }
            if (topN < 1) {
                topN = 10;
            }

            List<Map<String, Object>> rankList = filePreviewRankService.getTopN(topN);
            result.put("code", 0);
            result.put("msg", "success");
            result.put("data", rankList);
            result.put("total", rankList.size());
        } catch (Exception e) {
            LOGGER.error("获取文件预览排行榜失败，error：{}", e.getMessage());
            result.put("code", 500);
            result.put("msg", "获取排行榜失败：" + e.getMessage());
            result.put("data", null);
        }
        return result;
    }

    /**
     * 获取单个文件的预览次数
     *
     * @param fileName 文件名称
     * @return 预览次数
     */
    @GetMapping("/api/previewRank/count")
    @ResponseBody
    public Map<String, Object> getPreviewCount(@RequestParam String fileName) {
        Map<String, Object> result = new HashMap<>();
        try {
            long count = filePreviewRankService.getPreviewCount(fileName);
            result.put("code", 0);
            result.put("msg", "success");
            result.put("data", count);
        } catch (Exception e) {
            LOGGER.error("获取文件预览次数失败，fileName：{}，error：{}", fileName, e.getMessage());
            result.put("code", 500);
            result.put("msg", "获取预览次数失败：" + e.getMessage());
        }
        return result;
    }
}
