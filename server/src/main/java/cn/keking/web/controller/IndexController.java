package cn.keking.web.controller;

import cn.keking.service.RankService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 *  页面跳转
 * @author yudian-it
 * @date 2017/12/27
 */
@Controller
public class IndexController {

    private final RankService rankService;

    public IndexController(RankService rankService) {
        this.rankService = rankService;
    }

    @GetMapping( "/index")
    public String go2Index(){
        return "/main/index";
    }

    @GetMapping( "/record")
    public String go2Record(){
        return "/main/record";
    }

    @GetMapping( "/sponsor")
    public String go2Sponsor(){
        return "/main/sponsor";
    }

    @GetMapping( "/integrated")
    public String go2Integrated(){
        return "/main/integrated";
    }

    @GetMapping( "/")
    public String root() {
        return "/main/index";
    }

    @GetMapping( "/popular-files")
    public String go2PopularFiles(){
        return "/main/popular-files";
    }

    @GetMapping( "/api/popular-files")
    @ResponseBody
    public Map<String, Long> getPopularFiles(@RequestParam(defaultValue = "10") int topN){
        return rankService.getFilePreviewTop(topN);
    }

}
