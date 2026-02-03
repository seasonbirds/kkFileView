package cn.keking.web.filter;

import cn.keking.service.FilePreviewRankService;
import cn.keking.utils.WebUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;

/**
 * 文件预览排行榜统计过滤器
 * 通过Filter拦截onlinePreview请求，实现预览次数统计
 * <p>
 * 高并发场景下的计数准确性保证：
 * 1. Filter在请求处理完成后进行统计，确保预览成功才计数
 * 2. 使用Redis Sorted Set的incrementScore原子操作
 * 3. Redis单线程执行命令，天然保证并发安全
 *
 * @author kl
 * @since 2024/01/01
 */
public class FilePreviewRankFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilePreviewRankFilter.class);

    private FilePreviewRankService filePreviewRankService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 从Spring上下文获取Service
        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
        if (context != null) {
            this.filePreviewRankService = context.getBean(FilePreviewRankService.class);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String encodedUrl = httpRequest.getParameter("url");
        String fileName = extractFileName(encodedUrl);

        try {
            chain.doFilter(request, response);
            // 请求成功完成后进行统计
            if (fileName != null && filePreviewRankService != null) {
                filePreviewRankService.incrementPreviewCount(fileName);
            }
        } catch (Exception e) {
            LOGGER.debug("文件预览统计失败，fileName：{}，error：{}", fileName, e.getMessage());
            throw e;
        }
    }

    @Override
    public void destroy() {
        // 销毁操作
    }

    /**
     * 从URL中提取文件名
     */
    private String extractFileName(String encodedUrl) {
        if (encodedUrl == null || encodedUrl.isEmpty()) {
            return null;
        }
        try {
            String fileUrl = WebUtils.decodeUrl(encodedUrl);
            int lastSlashIndex = fileUrl.lastIndexOf('/');
            int lastBackslashIndex = fileUrl.lastIndexOf('\\');
            int lastIndex = Math.max(lastSlashIndex, lastBackslashIndex);

            if (lastIndex > 0 && lastIndex < fileUrl.length() - 1) {
                String fileName = fileUrl.substring(lastIndex + 1);
                int queryIndex = fileName.indexOf('?');
                if (queryIndex > 0) {
                    fileName = fileName.substring(0, queryIndex);
                }
                return java.net.URLDecoder.decode(fileName, "UTF-8");
            }
        } catch (Exception e) {
            LOGGER.debug("提取文件名失败：{}", e.getMessage());
        }
        return null;
    }
}
