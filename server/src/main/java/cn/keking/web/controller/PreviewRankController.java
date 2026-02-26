package cn.keking.web.controller;

import cn.keking.service.PreviewRankService;
import cn.keking.service.PreviewRankService.PreviewRankItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 预览排行榜控制器
 * 提供排行榜页面展示和数据查询接口
 */
@Controller
public class PreviewRankController {

    @Autowired
    private PreviewRankService previewRankService;

    /**
     * 排行榜页面
     *
     * @param top   默认展示的排名数量
     * @param model 模型对象
     * @return 页面路径
     */
    @GetMapping("/rank")
    public String rankPage(@RequestParam(value = "top", defaultValue = "10") int top, Model model) {
        model.addAttribute("top", top);
        return "/main/rank";
    }

    /**
     * 获取排行榜数据接口
     * 返回JSON格式的排行榜数据，供前端页面调用
     *
     * @param top 返回的记录数量，默认10，最大100
     * @return 排行榜列表
     */
    @GetMapping("/rank/list")
    @ResponseBody
    public List<PreviewRankItem> getRankList(@RequestParam(value = "top", defaultValue = "10") int top) {
        // 参数校验：确保top在有效范围内
        if (top <= 0) {
            top = 10;
        }
        if (top > 100) {
            top = 100;
        }
        // 查询排行榜数据
        List<PreviewRankItem> rankList = previewRankService.getTopRankList(top);
        // 设置排名序号
        int rank = 1;
        for (PreviewRankItem item : rankList) {
            item.setRank(rank++);
        }
        return rankList;
    }
}
