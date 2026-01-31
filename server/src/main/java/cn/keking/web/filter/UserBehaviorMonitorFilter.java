package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import cn.keking.service.userbehavior.AlertEmailService;
import cn.keking.service.userbehavior.UserBehaviorAccessManager;
import cn.keking.service.userbehavior.UserBehaviorService;
import cn.keking.service.userbehavior.UserBehaviorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用户行为监控过滤器
 * 负责拦截预览请求，进行访问频率检查和行为记录
 *
 * 核心职责：
 * 1. 检查用户行为监控功能是否启用
 * 2. 过滤需要监控的URL（onlinePreview、picturesPreview）
 * 3. 调用AccessManager进行访问限制检查和行为记录
 * 4. 处理访问限制响应
 * 5. 异常时不阻断正常请求（容错设计）
 *
 * 依赖关系：
 * - UserBehaviorUtils: 提供IP获取、文件名提取等工具方法
 * - UserBehaviorAccessManager: 提供访问限制检查和行为记录的核心逻辑
 * - UserBehaviorService: 数据库操作（异步）
 * - AlertEmailService: 告警邮件发送（异步）
 */
public class UserBehaviorMonitorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorMonitorFilter.class);

    private UserBehaviorService userBehaviorService;
    private AlertEmailService alertEmailService;

    /**
     * Filter初始化方法
     * 通过Spring WebApplicationContext获取所需的Service Bean
     *
     * @param filterConfig Filter配置
     * @throws ServletException 初始化异常
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
        if (context != null) {
            userBehaviorService = context.getBean(UserBehaviorService.class);
            alertEmailService = context.getBean(AlertEmailService.class);
        }
        logger.info("UserBehaviorMonitorFilter initialized");
    }

    /**
     * 请求过滤核心方法
     * 执行流程：
     * 1. 检查功能开关，未启用则直接放行
     * 2. 检查URL是否需要监控，不需要则放行
     * 3. 检查IP是否已被限制（日限制或周期限制），已限制则返回403
     * 4. 调用AccessManager记录行为并检查限制
     * 5. 检查结果触发限制则返回403，否则放行
     * 6. 任何异常都不阻断请求（容错设计）
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param chain    Filter链
     * @throws IOException      IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            if (!ConfigConstants.isUserBehaviorMonitorEnabled()) {
                chain.doFilter(request, response);
                return;
            }

            String requestUri = httpRequest.getRequestURI();
            if (!UserBehaviorUtils.isMonitorUrl(requestUri)) {
                chain.doFilter(request, response);
                return;
            }

            String ipAddress = UserBehaviorUtils.getClientIpAddress(httpRequest);

            if (UserBehaviorAccessManager.isBlockedDaily(ipAddress)) {
                sendForbiddenResponse(httpResponse, "用户行为异常，不能继续访问系统，请联系管理员！");
                return;
            }

            if (UserBehaviorAccessManager.isBlockedPeriod(ipAddress)) {
                sendForbiddenResponse(httpResponse, "请求太频繁，请稍后再试！");
                return;
            }

            String fileName = UserBehaviorUtils.getFileName(httpRequest);

            UserBehaviorAccessManager.AccessCheckResult result = new UserBehaviorAccessManager.AccessCheckResult();
            UserBehaviorAccessManager.recordAndCheck(ipAddress, fileName, userBehaviorService, alertEmailService, result);

            if (result.isPeriodBlocked()) {
                sendForbiddenResponse(httpResponse, "请求太频繁，请稍后再试！");
                return;
            }

            if (result.isDailyBlocked()) {
                sendForbiddenResponse(httpResponse, "用户行为异常，不能继续访问系统，请联系管理员！");
                return;
            }

        } catch (Exception e) {
            logger.error("User behavior monitor error, allowing request to proceed: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }

    /**
     * 发送403禁止访问响应
     *
     * @param response HTTP响应对象
     * @param message  提示消息
     * @throws IOException IO异常
     */
    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setHeader("Content-Type", "text/html; charset=UTF-8");
        response.getWriter().println(message);
    }

    /**
     * Filter销毁方法
     * 调用AccessManager.shutdown()关闭异步线程池
     */
    @Override
    public void destroy() {
        UserBehaviorAccessManager.shutdown();
    }
}