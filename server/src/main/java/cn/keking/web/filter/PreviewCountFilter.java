package cn.keking.web.filter;

import cn.keking.service.PreviewCountService;
import cn.keking.utils.WebUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 预览次数统计过滤器
 * 拦截/onlinePreview和/picturesPreview请求，统计文件预览次数
 * 
 * @author Trae AI
 * @since 2024/03/29
 */
public class PreviewCountFilter implements Filter {

    private final Logger logger = LoggerFactory.getLogger(PreviewCountFilter.class);

    private static final List<String> TARGET_PATHS = Arrays.asList("/onlinePreview", "/picturesPreview");

    @Autowired
    private PreviewCountService previewCountService;

    @Override
    public void init(FilterConfig filterConfig) {
        // 初始化逻辑
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) 
            throws IOException, ServletException {
        
        if (previewCountService != null) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String requestPath = httpRequest.getServletPath();
            
            if (TARGET_PATHS.contains(requestPath)) {
                try {
                    String url = httpRequest.getParameter("url");
                    String urls = httpRequest.getParameter("urls");
                    
                    if (requestPath.equals("/onlinePreview") && url != null) {
                        String decodedUrl = WebUtils.decodeUrl(url);
                        String fileName = WebUtils.getFileNameFromURL(decodedUrl);
                        previewCountService.incrementPreviewCount(decodedUrl, fileName);
                    } else if (requestPath.equals("/picturesPreview") && urls != null) {
                        String decodedUrls = WebUtils.decodeUrl(urls);
                        // 分割多个图片URL并分别统计
                        String[] urlArray = decodedUrls.split("\\|");
                        for (String imgUrl : urlArray) {
                            String trimmedUrl = imgUrl.trim();
                            String fileName = WebUtils.getFileNameFromURL(trimmedUrl);
                            previewCountService.incrementPreviewCount(trimmedUrl, fileName);
                        }
                    }
                } catch (Exception e) {
                    logger.error("统计预览次数失败", e);
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 销毁逻辑
    }
}
