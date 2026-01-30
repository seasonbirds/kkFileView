package cn.keking.web.controller;

import cn.keking.model.ReturnResponse;
import cn.keking.service.FileRankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

@Controller
@ConditionalOnBean(FileRankService.class)
public class FileRankController {

    private final FileRankService fileRankService;

    @Autowired
    public FileRankController(FileRankService fileRankService) {
        this.fileRankService = fileRankService;
    }

    @GetMapping("/fileRank")
    public String fileRankPage() {
        return "/main/fileRank";
    }

    @GetMapping("/api/fileRank")
    @ResponseBody
    public ReturnResponse<Object> getFileRank(
            @RequestParam(defaultValue = "10") int top) {
        if (top <= 0) {
            top = 10;
        }
        if (top > 100) {
            top = 100;
        }
        List<FileRankService.FileRankItem> items = fileRankService.getTopFiles(top);
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setRank(i + 1);
        }
        return ReturnResponse.success(items);
    }
}
