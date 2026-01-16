package cn.keking.web.controller;

import cn.keking.model.ReturnResponse;
import cn.keking.service.FilePreviewStatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class RankingController {

    private static final Logger logger = LoggerFactory.getLogger(RankingController.class);

    private final FilePreviewStatisticsService statisticsService;

    @Autowired
    public RankingController(FilePreviewStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/ranking/list")
    public ReturnResponse<List<Map<String, Object>>> getRankingList(@RequestParam(defaultValue = "10") int limit) {
        try {
            if (limit <= 0 || limit > 100) {
                limit = 10;
            }
            List<Map<String, Object>> rankingList = statisticsService.getTopPreviewFiles(limit);
            return ReturnResponse.successResponse(rankingList);
        } catch (Exception e) {
            logger.error("获取排行榜数据失败", e);
            return ReturnResponse.failureResponse("获取排行榜数据失败");
        }
    }
}
