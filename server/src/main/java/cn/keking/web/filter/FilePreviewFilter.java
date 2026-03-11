package cn.keking.web.filter;

import cn.keking.service.FileRankService;
import cn.keking.utils.WebUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;

/**
 * 文件预览统计过滤器
 * 用于拦截/onlinePreview接口，统计文件预览次数
 */
public class FilePreviewFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(FilePreviewFilter.class);
    private FileRankService fileRankService;

    @Override
    public void init(FilterConfig filterConfig) {
        ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
        if (context != null) {
            fileRankService = context.getBean(FileRankService.class);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
            // 请求成功后统计预览次数
            if (fileRankService != null) {
                String fileName = WebUtils.getFileNameFromRequest(request);
                if (fileName != null && !fileName.isEmpty()) {
                    fileRankService.incrementPreviewCount(fileName);
                }
            }
        } catch (Exception e) {
            logger.error("File preview filter error", e);
            throw e;
        }
    }

    @Override
    public void destroy() {
    }
}
