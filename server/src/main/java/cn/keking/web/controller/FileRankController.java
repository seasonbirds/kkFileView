package cn.keking.web.controller;

import cn.keking.service.FileRankService;
import cn.keking.web.filter.FileRankFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class FileRankController {

    private static final Logger logger = LoggerFactory.getLogger(FileRankController.class);

    @Autowired(required = false)
    private FileRankService fileRankService;

    @GetMapping("/rank")
    public String go2Rank() {
        return "/main/rank";
    }

    @GetMapping("/api/fileRank")
    @ResponseBody
    public Map<String, Object> getFileRank(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (fileRankService != null) {
                List<FileRankService.FileRankItem> items = fileRankService.getTopFiles(limit);
                result.put("code", 0);
                result.put("data", items);
            } else {
                result.put("code", 0);
                result.put("data", new ArrayList<>());
            }
        } catch (Exception e) {
            logger.error("Failed to get file rank", e);
            result.put("code", -1);
            result.put("message", "获取排行榜数据失败");
            result.put("data", new ArrayList<>());
        }
        return result;
    }
}