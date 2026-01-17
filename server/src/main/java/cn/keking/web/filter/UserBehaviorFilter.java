package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import cn.keking.service.UserBehaviorService;
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
import java.io.PrintWriter;

/**
 * 用户行为分析过滤器
 *
 * @author keking
 * @since 2025-07-17
 */
public class UserBehaviorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorFilter.class);
    private static final String PREVIEW_PATH = "onlinePreview";
    private static final String API_PATH = "/api/";

    private UserBehaviorService userBehaviorService;

    @Override
    public void init(FilterConfig filterConfig) {
        try {
            userBehaviorService = new UserBehaviorService();
            logger.info("用户行为分析过滤器初始化成功");
        } catch (Exception e) {
            logger.error("用户行为分析过滤器初始化失败", e);
            throw new RuntimeException("用户行为分析过滤器初始化失败", e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 检查功能是否启用
        if (!ConfigConstants.isUserBehaviorAnalysisEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestURI = httpRequest.getRequestURI();

        // 只处理预览相关的请求
        if (!shouldFilter(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        // 获取用户IP地址
        String ipAddress = getClientIpAddress(httpRequest);

        // 获取预览的文件名
        String sourceUrl = WebUtils.getSourceUrl(request);
        String fileName = sourceUrl != null ? WebUtils.getFileNameFromURL(sourceUrl) : null;

        // 处理用户行为，异常时允许正常访问
        UserBehaviorService.UserBehaviorResult result;
        try {
            result = userBehaviorService.handleRequest(ipAddress, fileName, requestURI);
        } catch (Exception e) {
            logger.error("用户行为分析处理异常，允许正常访问", e);
            chain.doFilter(request, response);
            return;
        }

        // 根据处理结果决定是否允许访问
        if (result == UserBehaviorService.UserBehaviorResult.ALLOWED) {
            chain.doFilter(request, response);
        } else {
            // 返回错误提示
            response.setContentType("text/html; charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write("<html><body><h1>" + result.getMessage() + "</h1></body></html>");
            writer.flush();
            writer.close();
            logger.warn("IP地址 {} 被阻止访问，原因: {}", ipAddress, result.getMessage());
        }
    }

    /**
     * 判断是否需要过滤该请求
     *
     * @param requestURI 请求URI
     * @return 需要过滤返回true，否则返回false
     */
    private boolean shouldFilter(String requestURI) {
        // 只过滤在线预览相关的请求
        return requestURI.contains(PREVIEW_PATH) || requestURI.startsWith(API_PATH);
    }

    /**
     * 获取客户端真实IP地址
     *
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多级代理，取第一个IP地址
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Override
    public void destroy() {
        logger.info("用户行为分析过滤器已销毁");
    }
}
