package cn.keking.web.filter;

import cn.keking.service.FileRankService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;

@Component
@ConditionalOnBean(FileRankService.class)
public class FileRankFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(FileRankFilter.class);

    @Autowired
    private FileRankService fileRankService;

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        filterChain.doFilter(request, response);
        if (fileRankService != null) {
            try {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                String url = httpRequest.getParameter("url");
                if (url != null && !url.isEmpty()) {
                    String decodedUrl = URLDecoder.decode(url, "UTF-8");
                    String fileName = extractFileNameFromUrl(decodedUrl);
                    fileRankService.incrementPreviewCount(decodedUrl, fileName);
                }
            } catch (Exception e) {
                logger.debug("Failed to record preview rank", e);
            }
        }
    }

    private String extractFileNameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "unknown";
        }
        int lastSlash = fileUrl.lastIndexOf('/');
        int lastBackslash = fileUrl.lastIndexOf('\\');
        int pos = Math.max(lastSlash, lastBackslash);
        if (pos >= 0 && pos < fileUrl.length() - 1) {
            return fileUrl.substring(pos + 1);
        }
        return fileUrl;
    }

    @Override
    public void destroy() {
    }
}
