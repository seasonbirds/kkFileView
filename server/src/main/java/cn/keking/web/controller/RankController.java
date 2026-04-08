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

@Controller
public class RankController {

    private final RankService rankService;

    public RankController(RankService rankService) {
        this.rankService = rankService;
    }

    @GetMapping("/rank")
    public String rankPage(Model model, 
                           @RequestParam(value = "topN", defaultValue = "10") int topN) {
        model.addAttribute("rankEnabled", rankService.isEnabled());
        model.addAttribute("topN", topN);
        return "/main/rank";
    }

    @GetMapping("/api/rank")
    @ResponseBody
    public Map<String, Object> getRankData(@RequestParam(value = "topN", defaultValue = "10") int topN) {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", rankService.isEnabled());
        
        if (rankService.isEnabled()) {
            if (topN <= 0) {
                topN = 10;
            } else if (topN > 1000) {
                topN = 1000;
            }
            List<RankService.RankItem> rankItems = rankService.getTopFiles(topN);
            result.put("data", rankItems);
            result.put("total", rankItems.size());
        }
        
        return result;
    }
}
