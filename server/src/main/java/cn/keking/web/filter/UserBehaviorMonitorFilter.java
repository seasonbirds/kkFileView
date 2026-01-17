package cn.keking.web.filter;

import cn.keking.security.config.SecurityConfig;

import cn.keking.security.service.UserBehaviorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用户行为监控过滤器
 * 实现无侵入式的用户行为监控和访问频率限制
 * 拦截预览请求，检查访问频率，限制异常请求
 */
public class UserBehaviorMonitorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorMonitorFilter.class);

    /**
     * 用户行为服务
     */
    private final UserBehaviorService userBehaviorService;

    /**
     * 安全配置
     */
    private final SecurityConfig securityConfig;

    public UserBehaviorMonitorFilter(UserBehaviorService userBehaviorService,
                                     SecurityConfig securityConfig) {
        this.userBehaviorService = userBehaviorService;
        this.securityConfig = securityConfig;
    }

    /**
     * 初始化过滤器
     * @param filterConfig 过滤器配置
     */
    @Override
    public void init(FilterConfig filterConfig) {
        logger.info("UserBehaviorMonitorFilter initialized");
    }

    /**
     * 执行过滤逻辑
     * 拦截预览请求，检查访问频率，限制异常请求
     * 异常时允许请求继续，不影响正常访问
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
        if (!securityConfig.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String requestUri = httpRequest.getRequestURI();
            if (!isPreviewRequest(requestUri)) {
                chain.doFilter(request, response);
                return;
            }

            String ipAddress = getClientIpAddress(httpRequest);
            String fileName = extractFileName(httpRequest);

            UserBehaviorService.CheckResult checkResult = userBehaviorService.checkAndRecordRequest(ipAddress, fileName);

            if (!checkResult.isAllowed()) {
                httpResponse.setContentType("text/plain;charset=UTF-8");
                httpResponse.getWriter().write(checkResult.getMessage());
                httpResponse.getWriter().flush();
                return;
            }

            chain.doFilter(request, response);
        } catch (Exception e) {
            // 异常时允许请求继续，不影响正常访问
            logger.error("Error in UserBehaviorMonitorFilter, allowing request to proceed", e);
            chain.doFilter(request, response);
        }
    }

    /**
     * 判断是否为预览请求
     * @param requestUri 请求URI
     * @return 是否为预览请求
     */
    private boolean isPreviewRequest(String requestUri) {
        return requestUri.contains("/onlinePreview") || requestUri.contains("/picturesPreview");
    }

    /**
     * 获取客户端IP地址
     * 支持反向代理场景，从X-Forwarded-For和X-Real-IP头获取
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 从请求中提取文件名
     * @param request HTTP请求
     * @return 文件名
     */
    private String extractFileName(HttpServletRequest request) {
        String url = request.getParameter("url");
        if (url != null && !url.isEmpty()) {
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < url.length() - 1) {
                return url.substring(lastSlash + 1);
            }
            return url;
        }
        return "unknown";
    }

    /**
     * 销毁过滤器
     */
    @Override
    public void destroy() {
        logger.info("UserBehaviorMonitorFilter destroyed");
    }
}
