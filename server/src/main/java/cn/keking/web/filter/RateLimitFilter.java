package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import cn.keking.utils.rate.RateLimiter;
import cn.keking.utils.rate.RateLimiterFactory;
import cn.keking.utils.WebUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 基于IP地址的限流Filter
 */
public class RateLimitFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    private RateLimiter rateLimiter;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化限流器
        rateLimiter = RateLimiterFactory.getInstance().getRateLimiter();
        // 从配置文件读取限流参数
        int period = ConfigConstants.getRateLimitPeriod();
        int limit = ConfigConstants.getRateLimitCount();
        rateLimiter.setLimit(period, limit);
        logger.info("RateLimitFilter initialized with period: {}ms, limit: {}", period, limit);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            // 获取真实IP地址
            String ip = WebUtils.getClientIP(httpRequest);
            
            // 检查是否允许访问
            if (!rateLimiter.allow(ip)) {
                // 限流，返回提示信息
                httpResponse.setContentType("text/html;charset=UTF-8");
                httpResponse.getWriter().write("请求太频繁，请稍后再试");
                httpResponse.getWriter().flush();
                logger.warn("Rate limit exceeded for IP: {}", ip);
                return;
            }
            
            // 允许访问，继续执行
            chain.doFilter(request, response);
        } catch (Exception e) {
            // 异常时允许访问，避免影响正常功能
            logger.error("RateLimitFilter error", e);
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        // 清理资源
    }
}
