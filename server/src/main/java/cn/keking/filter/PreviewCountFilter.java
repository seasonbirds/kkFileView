package cn.keking.filter;

import cn.keking.service.cache.CacheService;
import cn.keking.utils.FileUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class PreviewCountFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(PreviewCountFilter.class);

    private CacheService cacheService;

    public void setCacheService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String url = httpRequest.getParameter("url");
        
        if (url != null && !url.isEmpty()) {
            try {
                String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
                String fileName = FileUtils.getFileName(decodedUrl);
                
                if (fileName != null && !fileName.isEmpty() && cacheService != null) {
                    cacheService.incrementPreviewCount(fileName);
                }
            } catch (Exception e) {
                logger.error("统计预览次数失败", e);
            }
        }
        
        chain.doFilter(request, response);
    }
}
