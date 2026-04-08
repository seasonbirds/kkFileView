package cn.keking.web.controller;

import cn.keking.service.RankService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件预览排行榜Controller
 * 提供排行榜页面和API接口
 *
 * @author kkFileView
 */
@Controller
public class RankController {

    private final RankService rankService;

    public RankController(RankService rankService) {
        this.rankService = rankService;
    }

    /**
     * 排行榜页面路由
     *
     * @param model 模型对象
     * @param topN 显示前N名，默认10
     * @return 页面视图
     */
    @GetMapping("/rank")
    public String rankPage(Model model,
                           @RequestParam(value = "topN", defaultValue = "10") int topN) {
        model.addAttribute("topN", topN);
        return "/main/rank";
    }

    /**
     * 排行榜数据API接口
     *
     * @param topN 获取前N名，默认10
     * @return 排行榜数据
     */
    @GetMapping("/api/rank")
    @ResponseBody
    public Map<String, Object> getRankData(@RequestParam(value = "topN", defaultValue = "10") int topN) {
        Map<String, Object> result = new HashMap<>();

        if (topN <= 0) {
            topN = 10;
        } else if (topN > 1000) {
            topN = 1000;
        }

        List<RankService.RankItem> rankItems = rankService.getTopFiles(topN);
        result.put("data", rankItems);
        result.put("total", rankItems.size());

        return result;
    }
}
