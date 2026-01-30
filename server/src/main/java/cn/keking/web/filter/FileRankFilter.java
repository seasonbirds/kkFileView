package cn.keking.web.filter;

import cn.keking.service.FileRankService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;

public class FileRankFilter implements Filter {

    private FileRankService fileRankService;

    @Override
    public void init(FilterConfig filterConfig) {
        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
        if (context != null) {
            try {
                fileRankService = context.getBean(FileRankService.class);
            } catch (Exception e) {
                fileRankService = null;
            }
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        filterChain.doFilter(request, response);
        
        if (fileRankService != null) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String url = httpRequest.getParameter("url");
            if (url != null && !url.isEmpty()) {
                fileRankService.incrementPreviewCount(url);
            }
        }
    }

    @Override
    public void destroy() {
    }
}
