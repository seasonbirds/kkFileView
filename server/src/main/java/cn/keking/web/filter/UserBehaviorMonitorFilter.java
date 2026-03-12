package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import cn.keking.service.monitor.UserBehaviorMonitorService;
import cn.keking.utils.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用户行为监控过滤器
 * 用于记录用户访问行为并进行访问频率限制
 * 过滤器出错时不会影响用户正常访问系统
 */
public class UserBehaviorMonitorFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorMonitorFilter.class);

    private UserBehaviorMonitorService userBehaviorMonitorService;

    /**
     * 过滤器初始化方法
     *
     * @param filterConfig 过滤器配置
     */
    @Override
    public void init(FilterConfig filterConfig) {
        try {
            ServletContext servletContext = filterConfig.getServletContext();
            ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(servletContext);
            if (context != null) {
                userBehaviorMonitorService = context.getBean(UserBehaviorMonitorService.class);
            }
        } catch (Exception e) {
            logger.error("Failed to initialize UserBehaviorMonitorFilter", e);
            // 初始化失败时不抛出异常，保证系统正常启动
        }
    }

    /**
     * 执行过滤逻辑
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param chain    过滤器链
     * @throws IOException      IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // 如果监控未启用或服务未初始化，直接放行
            if (!ConfigConstants.isBehaviorMonitorEnabled() || userBehaviorMonitorService == null) {
                chain.doFilter(request, response);
                return;
            }

            String ipAddress = WebUtils.getClientIpAddress(httpRequest);
            String fileName = httpRequest.getParameter("fileName");

            // 记录访问
            userBehaviorMonitorService.recordAccess(ipAddress, fileName);

            // 检查限流
            if (userBehaviorMonitorService.isRateLimited(ipAddress)) {
                // 发送告警邮件
                userBehaviorMonitorService.sendAlertIfNeeded(ipAddress);
                // 返回限流页面
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/ratelimit.html");
                return;
            }

            // 检查每日限制
            if (userBehaviorMonitorService.isDailyLimitExceeded(ipAddress)) {
                httpResponse.sendRedirect(httpRequest.getContextPath() + "/dailylimit.html");
                return;
            }
        } catch (Exception e) {
            // 捕获所有异常，记录日志但不影响用户正常访问
            logger.error("Error in UserBehaviorMonitorFilter, continuing with normal request processing", e);
        }

        // 继续执行过滤器链
        chain.doFilter(request, response);
    }

    /**
     * 过滤器销毁方法
     */
    @Override
    public void destroy() {
        // 清理资源
    }
}
