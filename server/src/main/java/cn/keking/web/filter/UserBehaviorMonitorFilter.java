package cn.keking.web.filter;

import cn.keking.service.AlertEmailService;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户行为监控过滤器
 * 用于监控和限制用户的预览请求频率
 * @author kkfileview
 */
public class UserBehaviorMonitorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorMonitorFilter.class);

    private final UserBehaviorService userBehaviorService;
    private final AlertEmailService alertEmailService;
    private final Boolean monitorEnabled;
    private final Integer windowMinutes;
    private final Integer windowMaxRequests;
    private final Integer dailyMaxRequests;

    private String rateLimitHtmlView;
    private String blockedHtmlView;

    private final Set<String> alertedIps = ConcurrentHashMap.newKeySet();

    public UserBehaviorMonitorFilter(UserBehaviorService userBehaviorService, AlertEmailService alertEmailService,
                                     Boolean monitorEnabled, Integer windowMinutes, 
                                     Integer windowMaxRequests, Integer dailyMaxRequests) {
        this.userBehaviorService = userBehaviorService;
        this.alertEmailService = alertEmailService;
        this.monitorEnabled = monitorEnabled;
        this.windowMinutes = windowMinutes != null ? windowMinutes : 5;
        this.windowMaxRequests = windowMaxRequests != null ? windowMaxRequests : 100;
        this.dailyMaxRequests = dailyMaxRequests != null ? dailyMaxRequests : 1000;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            ClassPathResource rateLimitResource = new ClassPathResource("web/rateLimit.html");
            byte[] rateLimitBytes = FileCopyUtils.copyToByteArray(rateLimitResource.getInputStream());
            this.rateLimitHtmlView = new String(rateLimitBytes, StandardCharsets.UTF_8);

            ClassPathResource blockedResource = new ClassPathResource("web/blocked.html");
            byte[] blockedBytes = FileCopyUtils.copyToByteArray(blockedResource.getInputStream());
            this.blockedHtmlView = new String(blockedBytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to load rate limit or blocked html files", e);
            this.rateLimitHtmlView = "<html><body><h1>请求太频繁，请稍后再试！</h1></body></html>";
            this.blockedHtmlView = "<html><body><h1>用户行为异常，不能继续访问系统，请联系管理员！</h1></body></html>";
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!Boolean.TRUE.equals(monitorEnabled)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            String ipAddress = WebUtils.getClientIp(httpRequest);
            String url = WebUtils.getSourceUrl(request);
            String fileName = extractFileName(url);

            if (userBehaviorService.isDailyLimited(ipAddress, dailyMaxRequests)) {
                logger.warn("IP {} 已达到每日访问限制，拒绝访问", ipAddress);
                httpResponse.setContentType("text/html;charset=UTF-8");
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.getWriter().write(blockedHtmlView);
                return;
            }

            if (userBehaviorService.isRateLimited(ipAddress, windowMinutes, windowMaxRequests)) {
                logger.warn("IP {} 在{}分钟内请求超过{}次，触发频率限制", ipAddress, windowMinutes, windowMaxRequests);

                if (!alertedIps.contains(ipAddress)) {
                    alertedIps.add(ipAddress);
                    userBehaviorService.incrementAlertCount(ipAddress);

                    int requestCount = userBehaviorService.getRequestCountInWindow(ipAddress, windowMinutes);
                    alertEmailService.sendAbnormalBehaviorAlert(ipAddress, windowMinutes, requestCount);
                }

                httpResponse.setContentType("text/html;charset=UTF-8");
                httpResponse.setStatus(429);
                httpResponse.getWriter().write(rateLimitHtmlView);
                return;
            }

            userBehaviorService.logRequest(ipAddress, fileName, httpRequest.getRequestURI() + (url != null ? "?url=" + url : ""));
        } catch (Exception e) {
            logger.error("用户行为监控处理异常，放行请求", e);
        }

        chain.doFilter(request, response);
    }

    private String extractFileName(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        try {
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < url.length() - 1) {
                String fileName = url.substring(lastSlash + 1);
                int queryIndex = fileName.indexOf('?');
                if (queryIndex > 0) {
                    fileName = fileName.substring(0, queryIndex);
                }
                return fileName;
            }
        } catch (Exception e) {
            logger.debug("提取文件名失败: {}", url);
        }
        return url;
    }

    @Override
    public void destroy() {
        alertedIps.clear();
    }
}
