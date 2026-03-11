package cn.keking.web.controller;

import cn.keking.model.ReturnResponse;
import cn.keking.service.FileRankService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文件排行榜Controller
 * 提供文件预览排行榜查询API
 */
@RestController
@RequestMapping("/api/rank")
public class FileRankController {

    private final FileRankService fileRankService;

    public FileRankController(FileRankService fileRankService) {
        this.fileRankService = fileRankService;
    }

    /**
     * 获取文件预览排行榜
     *
     * @param topN 前N名，默认10
     * @return 排行榜数据
     */
    @GetMapping("/files")
    public ReturnResponse<List<FileRankService.FileRankVO>> getFileRankList(
            @RequestParam(defaultValue = "10") int topN) {
        // 限制最大查询数量
        if (topN <= 0) {
            topN = 10;
        } else if (topN > 1000) {
            topN = 1000;
        }
        List<FileRankService.FileRankVO> list = fileRankService.getTopFiles(topN);
        return ReturnResponse.successWithType(list);
    }
}
