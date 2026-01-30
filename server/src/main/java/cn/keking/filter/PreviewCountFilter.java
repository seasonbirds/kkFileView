package cn.keking.filter;

import cn.keking.service.PreviewCountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 文件预览次数统计过滤器
 * @author kkfileview
 */
@Component
public class PreviewCountFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(PreviewCountFilter.class);
    
    @Autowired
    private PreviewCountService previewCountService;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("文件预览次数统计过滤器初始化");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();
        
        // 检查是否是预览请求
        if ("/onlinePreview".equals(requestURI) && "GET".equals(httpRequest.getMethod())) {
            // 获取URL参数
            String url = httpRequest.getParameter("url");
            
            if (url != null && !url.isEmpty()) {
                try {
                    // 使用异步方式记录预览次数，避免影响主流程性能
                    new Thread(() -> {
                        try {
                            previewCountService.incrementPreviewCount(url);
                            logger.debug("记录文件预览次数: {}", url);
                        } catch (Exception e) {
                            logger.error("记录文件预览次数失败: {}", url, e);
                        }
                    }).start();
                } catch (Exception e) {
                    logger.error("启动预览次数记录线程失败", e);
                }
            }
        }
        
        // 继续执行后续过滤器
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
        logger.info("文件预览次数统计过滤器销毁");
    }
}