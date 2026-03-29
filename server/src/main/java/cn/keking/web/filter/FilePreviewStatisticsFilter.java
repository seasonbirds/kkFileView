package cn.keking.web.filter;

import cn.keking.utils.WebUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;

/**
 * 文件预览统计Filter
 * 用于统计文件预览次数，非侵入式统计
 */
public class FilePreviewStatisticsFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(FilePreviewStatisticsFilter.class);
    private static final String STATISTICS_REDIS_KEY = "file:preview:statistics";
    private RedissonClient statisticsRedissonClient;

    @Override
    public void init(FilterConfig filterConfig) {
        try {
            ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
            if (context != null) {
                statisticsRedissonClient = context.getBean("statisticsRedissonClient", RedissonClient.class);
            }
        } catch (Exception e) {
            logger.error("Failed to initialize statistics Redisson client", e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            String url = WebUtils.getSourceUrl(request);
            if (url != null && !url.isEmpty()) {
                String fileName = WebUtils.getFileNameFromURL(url);
                if (fileName != null && !fileName.isEmpty()) {
                    try {
                        RScoredSortedSet<String> sortedSet = statisticsRedissonClient.getScoredSortedSet(STATISTICS_REDIS_KEY);
                        sortedSet.addScore(fileName, 1.0);
                        logger.debug("Statistics updated for file: {}, preview count: {}", fileName, sortedSet.getScore(fileName));
                    } catch (Exception e) {
                        logger.error("Failed to update statistics for file: {}", fileName, e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in file preview statistics", e);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
