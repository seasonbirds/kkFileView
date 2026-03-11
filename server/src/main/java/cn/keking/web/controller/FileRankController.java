package cn.keking.web.controller;

import cn.keking.model.ReturnResponse;
import cn.keking.service.FileRankService;
import cn.keking.service.FileRankService.FileRankVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 文件排行榜控制器
 * 提供排行榜API接口和页面跳转
 */
@Controller
public class FileRankController {

    private final Logger logger = LoggerFactory.getLogger(FileRankController.class);

    private final FileRankService fileRankService;

    public FileRankController(FileRankService fileRankService) {
        this.fileRankService = fileRankService;
    }

    /**
     * 跳转到排行榜页面
     */
    @GetMapping("/rank")
    public String go2Rank() {
        return "/main/fileRank";
    }

    /**
     * 获取TopN文件排行榜API
     *
     * @param topN 前N名（默认10）
     */
    @ResponseBody
    @RequestMapping("/api/fileRank")
    public ReturnResponse<List<FileRankVO>> getFileRank(
            @RequestParam(defaultValue = "10") int topN) {
        try {
            // 限制最大返回100条
            if (topN <= 0 || topN > 100) {
                topN = 100;
            }
            List<FileRankVO> rankList = fileRankService.getTopFiles(topN);
            return ReturnResponse.success(rankList);
        } catch (Exception e) {
            logger.error("获取文件排行榜失败", e);
            return ReturnResponse.failure("获取排行榜失败: " + e.getMessage());
        }
    }
}
