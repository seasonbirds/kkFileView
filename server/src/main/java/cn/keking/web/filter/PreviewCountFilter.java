package cn.keking.web.filter;

import cn.keking.service.RankService;
import cn.keking.utils.WebUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class PreviewCountFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(PreviewCountFilter.class);

    private final RankService rankService;

    public PreviewCountFilter(RankService rankService) {
        this.rankService = rankService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (!rankService.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestUri = httpRequest.getRequestURI();
        
        if ("/onlinePreview".equals(requestUri) || requestUri.endsWith("/onlinePreview")) {
            String urlParam = httpRequest.getParameter("url");
            if (urlParam != null && !urlParam.isEmpty()) {
                try {
                    String fileUrl = WebUtils.decodeUrl(urlParam);
                    if (fileUrl != null && !fileUrl.isEmpty()) {
                        String fileName = WebUtils.getFileNameFromURL(fileUrl);
                        filterChain.doFilter(request, response);
                        rankService.incrementPreviewCount(fileUrl, fileName);
                        return;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to decode url for preview count: {}", urlParam, e);
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
