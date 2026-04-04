package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import cn.keking.service.ratelimit.RateLimitCache;
import cn.keking.service.ratelimit.RateLimitCacheFactory;
import cn.keking.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * IP限流过滤器，用于限制单个IP在一定时间内的请求次数
 * 仅应用于 /onlinePreview 接口
 *
 * @author kkfileview
 * @since 2026-04-04
 */
public class IpRateLimitFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(IpRateLimitFilter.class);

    @Autowired
    private RateLimitCacheFactory rateLimitCacheFactory;

    private RateLimitCache rateLimitCache;

    /**
     * 初始化过滤器，创建限流缓存实例
     *
     * @param filterConfig 过滤器配置
     */
    @Override
    public void init(FilterConfig filterConfig) {
        try {
            rateLimitCache = rateLimitCacheFactory.getRateLimitCache();
            logger.info("IP限流过滤器初始化成功");
        } catch (Exception e) {
            logger.error("IP限流过滤器初始化失败，限流功能将不可用", e);
        }
    }

    /**
     * 执行过滤逻辑，检查请求是否被限流
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param filterChain 过滤器链
     * @throws IOException      IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        try {
            if (rateLimitCache == null) {
                filterChain.doFilter(request, response);
                return;
            }

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String clientIp = WebUtils.getClientIp(httpRequest);
            int maxRequests = ConfigConstants.getRateLimitMaxRequests();
            int periodSeconds = ConfigConstants.getRateLimitPeriodSeconds();

            if (!rateLimitCache.isAllowed(clientIp, maxRequests, periodSeconds)) {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.setContentType("text/plain;charset=UTF-8");
                try (PrintWriter writer = httpResponse.getWriter()) {
                    writer.write("请求太频繁，请稍后再试");
                }
                return;
            }
        } catch (Exception e) {
            logger.error("IP限流过滤器执行异常，将跳过限流检查", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 销毁过滤器，清理资源
     */
    @Override
    public void destroy() {
    }
}
