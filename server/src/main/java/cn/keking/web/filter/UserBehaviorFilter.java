package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import cn.keking.service.UserBehaviorService;
import cn.keking.utils.WebUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 用户行为监控过滤器
 * 用于拦截预览请求，记录用户行为并检测异常
 *
 * @author kkFileView
 */
public class UserBehaviorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorFilter.class);

    private UserBehaviorService userBehaviorService;
    private String blockedHtmlView;
    private String dailyLimitHtmlView;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
        // 加载拦截页面模板
        loadHtmlTemplates();
    }

    /**
     * 设置UserBehaviorService（通过WebConfig注入）
     */
    public void setUserBehaviorService(UserBehaviorService userBehaviorService) {
        this.userBehaviorService = userBehaviorService;
    }

    /**
     * 加载HTML模板
     */
    private void loadHtmlTemplates() {
        try {
            // 加载频率限制拦截页面
            ClassPathResource blockedResource = new ClassPathResource("web/blocked.html");
            if (blockedResource.exists()) {
                byte[] bytes = FileCopyUtils.copyToByteArray(blockedResource.getInputStream());
                this.blockedHtmlView = new String(bytes, StandardCharsets.UTF_8);
            } else {
                this.blockedHtmlView = buildDefaultBlockedPage("请求太频繁，请稍后再试！");
            }

            // 加载日访问限制拦截页面
            ClassPathResource dailyLimitResource = new ClassPathResource("web/dailyLimit.html");
            if (dailyLimitResource.exists()) {
                byte[] bytes = FileCopyUtils.copyToByteArray(dailyLimitResource.getInputStream());
                this.dailyLimitHtmlView = new String(bytes, StandardCharsets.UTF_8);
            } else {
                this.dailyLimitHtmlView = buildDefaultBlockedPage("用户行为异常，不能继续访问系统，请联系管理员！");
            }
        } catch (IOException e) {
            logger.error("加载拦截页面模板失败", e);
            this.blockedHtmlView = buildDefaultBlockedPage("请求太频繁，请稍后再试！");
            this.dailyLimitHtmlView = buildDefaultBlockedPage("用户行为异常，不能继续访问系统，请联系管理员！");
        }
    }

    /**
     * 构建默认拦截页面
     */
    private String buildDefaultBlockedPage(String message) {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>访问受限</title>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }\n" +
                "        .container { max-width: 600px; margin: 0 auto; }\n" +
                "        h1 { color: #e74c3c; }\n" +
                "        p { color: #666; font-size: 16px; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <h1>访问受限</h1>\n" +
                "        <p>" + message + "</p>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // 如果功能未启用，直接放行
        if (!ConfigConstants.isUserBehaviorMonitorEnabled() || userBehaviorService == null) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 只拦截预览相关的请求
        String requestUri = httpRequest.getRequestURI();
        if (!isPreviewRequest(requestUri)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 获取客户端IP
            String ipAddress = getClientIpAddress(httpRequest);

            // 检查访问限制
            String blockReason = userBehaviorService.checkAccessLimit(ipAddress);
            if (blockReason != null) {
                // 被拦截，返回相应页面
                httpResponse.setContentType("text/html;charset=UTF-8");
                httpResponse.setStatus(429); // SC_TOO_MANY_REQUESTS = 429

                String html;
                if (blockReason.contains("用户行为异常")) {
                    html = dailyLimitHtmlView.replace("${message}", blockReason)
                            .replace("${ip_address}", ipAddress);
                } else {
                    html = blockedHtmlView.replace("${message}", blockReason)
                            .replace("${ip_address}", ipAddress);
                }

                httpResponse.getWriter().write(html);
                httpResponse.getWriter().close();
                logger.warn("用户访问被拦截 - IP: {}, 原因: {}", ipAddress, blockReason);
                return;
            }

            // 记录访问日志（异步）
            try {
                String fileName = extractFileName(httpRequest);
                String requestUrl = httpRequest.getRequestURL().toString();
                String userAgent = httpRequest.getHeader("User-Agent");
                userBehaviorService.recordAccess(ipAddress, fileName, requestUrl, userAgent);
            } catch (Exception e) {
                // 记录日志失败不影响正常业务
                logger.debug("记录访问日志失败: {}", e.getMessage());
            }

        } catch (Exception e) {
            // Filter执行异常时，记录日志但不影响正常业务
            logger.error("用户行为监控Filter执行异常", e);
        }

        // 继续处理请求（无论监控逻辑是否成功，都不影响正常预览）
        chain.doFilter(request, response);
    }

    /**
     * 判断是否为预览请求
     */
    private boolean isPreviewRequest(String requestUri) {
        return requestUri != null && (
                requestUri.contains("/onlinePreview") ||
                        requestUri.contains("/picturesPreview") ||
                        requestUri.contains("/getCorsFile")
        );
    }

    /**
     * 获取客户端IP地址
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
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 从请求中提取文件名
     */
    private String extractFileName(HttpServletRequest request) {
        String url = request.getParameter("url");
        if (url == null || url.isEmpty()) {
            url = request.getParameter("urls");
        }
        if (url == null || url.isEmpty()) {
            return "unknown";
        }

        try {
            String decodedUrl = WebUtils.decodeUrl(url);
            if (decodedUrl != null && !decodedUrl.isEmpty()) {
                int lastSlash = decodedUrl.lastIndexOf('/');
                if (lastSlash >= 0 && lastSlash < decodedUrl.length() - 1) {
                    String fileName = decodedUrl.substring(lastSlash + 1);
                    // 移除查询参数
                    int queryIndex = fileName.indexOf('?');
                    if (queryIndex > 0) {
                        fileName = fileName.substring(0, queryIndex);
                    }
                    return fileName;
                }
            }
        } catch (Exception e) {
            logger.debug("提取文件名失败: {}", url);
        }

        return "unknown";
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
