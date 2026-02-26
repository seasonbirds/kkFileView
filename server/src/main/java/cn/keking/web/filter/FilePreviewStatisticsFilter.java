package cn.keking.web.filter;

import cn.keking.service.FilePreviewStatisticsService;
import cn.keking.utils.WebUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 文件预览统计过滤器
 * 拦截/onlinePreview请求，统计文件预览次数
 *
 * @author kl
 * @date 2026/02/25
 */
public class FilePreviewStatisticsFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(FilePreviewStatisticsFilter.class);

    private final FilePreviewStatisticsService statisticsService;

    public FilePreviewStatisticsFilter(FilePreviewStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("文件预览统计过滤器初始化完成");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestUri = httpRequest.getRequestURI();

        // 只处理/onlinePreview请求
        if (requestUri != null && requestUri.endsWith("/onlinePreview")) {
            String urlParam = httpRequest.getParameter("url");
            if (StringUtils.hasText(urlParam)) {
                try {
                    // 解码URL参数
                    String decodedUrl = WebUtils.decodeUrl(urlParam);
                    // 从URL中提取文件名
                    String fileName = extractFileName(decodedUrl);

                    // 继续执行过滤器链
                    chain.doFilter(request, response);

                    // 请求成功完成后，增加预览次数统计
                    // 放在doFilter之后，确保预览成功才统计
                    statisticsService.incrementPreviewCount(fileName, decodedUrl);
                    logger.debug("文件预览统计成功: {}", fileName);
                } catch (Exception e) {
                    // 统计失败不影响主流程
                    logger.error("文件预览统计失败", e);
                    chain.doFilter(request, response);
                }
            } else {
                chain.doFilter(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * 从URL中提取文件名
     *
     * @param fileUrl 文件URL
     * @return 文件名
     */
    private String extractFileName(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return "unknown";
        }

        try {
            // 处理fullfilename参数
            if (fileUrl.contains("fullfilename=")) {
                int start = fileUrl.indexOf("fullfilename=") + "fullfilename=".length();
                int end = fileUrl.indexOf("&", start);
                if (end == -1) {
                    end = fileUrl.length();
                }
                String fullFileName = fileUrl.substring(start, end);
                return URLDecoder.decode(fullFileName, StandardCharsets.UTF_8);
            }

            // 从URL路径中提取文件名
            String path = fileUrl;
            // 移除查询参数
            int queryIndex = path.indexOf("?");
            if (queryIndex != -1) {
                path = path.substring(0, queryIndex);
            }

            // 从路径中获取最后一部分作为文件名
            int lastSlash = path.lastIndexOf("/");
            if (lastSlash != -1 && lastSlash < path.length() - 1) {
                String fileName = path.substring(lastSlash + 1);
                // URL解码
                return URLDecoder.decode(fileName, StandardCharsets.UTF_8);
            }

            return path;
        } catch (Exception e) {
            logger.warn("提取文件名失败: {}", fileUrl, e);
            return "unknown";
        }
    }

    @Override
    public void destroy() {
        logger.info("文件预览统计过滤器已销毁");
    }
}
