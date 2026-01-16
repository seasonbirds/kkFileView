package cn.keking.web.filter;

import cn.keking.service.FilePreviewStatisticsService;
import cn.keking.utils.KkFileUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 文件预览统计过滤器
 * 用于拦截文件预览请求，统计文件预览次数
 * 
 * 实现原理：
 * 1. 拦截/onlinePreview接口请求
 * 2. 从请求参数中提取文件URL
 * 3. 解析文件名并异步记录预览次数
 * 4. 不影响原有请求处理流程
 * 
 * 设计要点：
 * - 使用异步处理避免阻塞主请求流程，确保对核心功能性能影响最小
 * - 只在请求成功处理后进行统计，避免统计失败请求
 * - 通过包装Response来获取处理结果，确保只统计成功预览
 * 
 * @author kkfileview
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "file.preview.statistics.enabled", havingValue = "true", matchIfMissing = true)
public class PreviewStatisticsFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(PreviewStatisticsFilter.class);

    private final FilePreviewStatisticsService statisticsService;
    private final ExecutorService asyncExecutor;

    @Autowired
    public PreviewStatisticsFilter(FilePreviewStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
        this.asyncExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "preview-statistics");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("文件预览统计过滤器初始化完成");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();

        if ("/onlinePreview".equals(requestURI)) {
            String url = request.getParameter("url");
            
            if (url != null && !url.isEmpty()) {
                StatisticsResponseWrapper wrappedResponse = new StatisticsResponseWrapper((jakarta.servlet.http.HttpServletResponse) response);
                
                try {
                    chain.doFilter(request, wrappedResponse);
                    
                    int status = wrappedResponse.getStatus();
                    if (status == 200) {
                        String fileName = extractFileName(url);
                        if (fileName != null && !fileName.isEmpty()) {
                            CompletableFuture.runAsync(() -> {
                                try {
                                    statisticsService.incrementPreviewCount(fileName);
                                } catch (Exception e) {
                                    logger.error("异步统计文件预览次数失败，fileName: {}", fileName, e);
                                }
                            }, asyncExecutor);
                        }
                    }
                } catch (Exception e) {
                    logger.error("文件预览统计过滤器处理异常", e);
                    throw e;
                }
            } else {
                chain.doFilter(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
        }
        logger.info("文件预览统计过滤器已销毁");
    }

    /**
     * 从文件URL中提取文件名
     * 
     * @param url 文件URL
     * @return 文件名
     */
    private String extractFileName(String url) {
        try {
            if (url == null || url.isEmpty()) {
                return null;
            }
            
            String decodedUrl = KkFileUtils.htmlDecode(url);
            int lastSlashIndex = decodedUrl.lastIndexOf('/');
            int lastBackslashIndex = decodedUrl.lastIndexOf('\\');
            int separatorIndex = Math.max(lastSlashIndex, lastBackslashIndex);
            
            if (separatorIndex >= 0 && separatorIndex < decodedUrl.length() - 1) {
                return decodedUrl.substring(separatorIndex + 1);
            }
            
            return decodedUrl;
        } catch (Exception e) {
            logger.error("提取文件名失败，url: {}", url, e);
            return null;
        }
    }
}