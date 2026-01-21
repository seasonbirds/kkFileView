package cn.keking.web.filter;

import cn.keking.model.FileAttribute;
import cn.keking.service.FileHandlerService;
import cn.keking.service.RankService;
import cn.keking.utils.WebUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 文件预览次数统计过滤器
 * 用于统计文件预览次数，避免对原有代码的侵入
 */
public class PreviewCountFilter implements Filter {

    private final Logger logger = LoggerFactory.getLogger(PreviewCountFilter.class);
    private FileHandlerService fileHandlerService;
    private RankService rankService;

    public PreviewCountFilter(FileHandlerService fileHandlerService, RankService rankService) {
        this.fileHandlerService = fileHandlerService;
        this.rankService = rankService;
    }

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        // 处理请求
        filterChain.doFilter(request, response);

        // 仅对/onlinePreview接口进行统计
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        if ("/onlinePreview".equals(httpRequest.getRequestURI())) {
            String url = httpRequest.getParameter("url");
            if (url != null) {
                try {
                    String fileUrl = WebUtils.decodeUrl(url);
                    FileAttribute fileAttribute = fileHandlerService.getFileAttribute(fileUrl, httpRequest);
                    // 增加文件预览次数统计
                    rankService.incrementFilePreviewCount(fileAttribute.getName());
                    logger.debug("统计文件预览次数: {}", fileAttribute.getName());
                } catch (Exception e) {
                    // 统计失败不影响正常预览
                    logger.error("统计文件预览次数失败", e);
                }
            }
        }
    }

    @Override
    public void destroy() {

    }
}