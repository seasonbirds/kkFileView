package cn.keking.web.filter;

import cn.keking.service.PreviewRankService;
import cn.keking.utils.WebUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 预览排行榜统计过滤器
 * 拦截/onlinePreview请求，在预览完成后异步记录统计数据
 * 采用无侵入式设计，不影响原有预览接口的代码和性能
 */
public class PreviewRankFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(PreviewRankFilter.class);

    private final PreviewRankService previewRankService;

    /**
     * 构造函数，注入预览排行榜服务
     *
     * @param previewRankService 预览排行榜服务
     */
    public PreviewRankFilter(PreviewRankService previewRankService) {
        this.previewRankService = previewRankService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("PreviewRankFilter initialized");
    }

    /**
     * 过滤器核心方法
     * 在请求处理完成后，异步记录预览统计信息
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 非HTTP请求直接放行
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // 获取预览文件的URL参数
        String urlParam = httpRequest.getParameter("url");
        // 无URL参数直接放行
        if (urlParam == null || urlParam.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        // 先执行原有预览逻辑
        chain.doFilter(request, response);

        // 预览完成后，异步记录统计数据
        try {
            // 解码URL参数
            String decodedUrl = WebUtils.decodeUrl(urlParam);
            if (decodedUrl != null) {
                // 从URL中提取文件名
                String fileName = extractFileName(decodedUrl);
                if (fileName != null && !fileName.isEmpty()) {
                    // 异步增加预览次数
                    previewRankService.incrementPreviewCount(fileName, decodedUrl);
                    logger.debug("Recorded preview for file: {}", fileName);
                }
            }
        } catch (Exception e) {
            // 统计失败不影响主流程，仅记录警告日志
            logger.warn("Failed to record preview count: {}", e.getMessage());
        }
    }

    /**
     * 从URL中提取文件名
     * 移除URL查询参数，获取最后一个路径段作为文件名
     *
     * @param url 文件URL
     * @return 文件名
     */
    private String extractFileName(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        String cleanUrl = url;
        // 移除查询参数
        int queryIndex = cleanUrl.indexOf('?');
        if (queryIndex > 0) {
            cleanUrl = cleanUrl.substring(0, queryIndex);
        }
        // 获取最后一个路径段作为文件名
        int lastSlashIndex = cleanUrl.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < cleanUrl.length() - 1) {
            return cleanUrl.substring(lastSlashIndex + 1);
        }
        return cleanUrl;
    }

    @Override
    public void destroy() {
    }
}
