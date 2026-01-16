package cn.keking.filter;

import cn.keking.service.FilePreviewCounterService;
import cn.keking.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 文件预览次数统计过滤器
 */
@WebFilter(urlPatterns = "/onlinePreview")
@Order(1)
public class FilePreviewCounterFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(FilePreviewCounterFilter.class);

    @Autowired
    private FilePreviewCounterService counterService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化逻辑
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String url = req.getParameter("url");
        
        // 继续处理请求
        chain.doFilter(request, response);
        
        // 在请求处理完成后统计预览次数
        try {
            if (url != null && !url.isEmpty()) {
                String fileUrl = WebUtils.decodeUrl(url);
                // 从URL中解析文件名
                String fileName = WebUtils.getFileNameFromUrl(fileUrl);
                if (fileName != null && !fileName.isEmpty()) {
                    counterService.incrementPreviewCount(fileName);
                }
            }
        } catch (Exception e) {
            logger.warn("统计文件预览次数失败：{}", e.getMessage());
        }
    }

    @Override
    public void destroy() {
        // 销毁逻辑
    }
}
