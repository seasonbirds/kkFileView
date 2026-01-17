package cn.keking.web.filter;

import cn.keking.service.AlertEmailService;
import cn.keking.service.UserAccessCountService;
import cn.keking.service.UserAccessLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 用户访问监控过滤器
 */
@Component
public class UserAccessMonitorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(UserAccessMonitorFilter.class);

    @Value("${user.monitor.enabled:true}")
    private boolean enabled;

    @Value("${user.monitor.rate.period.minutes:5}")
    private int ratePeriodMinutes;

    @Value("${user.monitor.rate.max.requests:100}")
    private int rateMaxRequests;

    @Value("${user.monitor.daily.max.requests:1000}")
    private int dailyMaxRequests;

    @Value("${user.monitor.email.to:admin@example.com}")
    private String alertEmail;

    private final UserAccessLogService userAccessLogService;
    private final UserAccessCountService userAccessCountService;
    private final AlertEmailService alertEmailService;

    @Autowired
    public UserAccessMonitorFilter(UserAccessLogService userAccessLogService,
                                   UserAccessCountService userAccessCountService,
                                   AlertEmailService alertEmailService) {
        this.userAccessLogService = userAccessLogService;
        this.userAccessCountService = userAccessCountService;
        this.alertEmailService = alertEmailService;
    }

    @PostConstruct
    public void init() {
        if (enabled) {
            userAccessLogService.initDatabase();
            logger.info("用户访问监控过滤器已启用");
        } else {
            logger.info("用户访问监控过滤器未启用");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String ipAddress = getClientIpAddress(httpRequest);
        String requestUri = httpRequest.getRequestURI();

        try {
            if (checkRateLimit(ipAddress, httpResponse)) {
                if (checkDailyLimit(ipAddress, httpResponse)) {
                    String fileName = extractFileName(requestUri);
                    userAccessLogService.recordAccessAsync(ipAddress, fileName);
                    chain.doFilter(request, response);
                }
            }
        } catch (Exception e) {
            logger.error("用户访问监控异常: IP={}, URI={}", ipAddress, requestUri, e);
            chain.doFilter(request, response);
        }
    }

    /**
     * 检查速率限制
     * @param ipAddress IP地址
     * @param response HTTP响应
     * @return 是否允许访问
     * @throws IOException IO异常
     */
    private boolean checkRateLimit(String ipAddress, HttpServletResponse response) throws IOException {
        String rateKey = "user:rate:" + ipAddress;
        long currentCount = userAccessCountService.incrementAndGet(rateKey);

        if (currentCount == 1) {
            userAccessCountService.expire(rateKey, ratePeriodMinutes * 60);
        }

        if (currentCount > rateMaxRequests) {
            logger.warn("用户访问频率过高: IP={}, 次数={}", ipAddress, currentCount);
            alertEmailService.sendAbnormalBehaviorAlert(alertEmail, ipAddress, ratePeriodMinutes, currentCount);
            response.setStatus(429);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("请求太频繁，请稍后再试！");
            return false;
        }

        return true;
    }

    /**
     * 检查每日限制
     * @param ipAddress IP地址
     * @param response HTTP响应
     * @return 是否允许访问
     * @throws IOException IO异常
     */
    private boolean checkDailyLimit(String ipAddress, HttpServletResponse response) throws IOException {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String dailyKey = "user:daily:" + ipAddress + ":" + today;
        long dailyCount = userAccessCountService.incrementAndGet(dailyKey);

        if (dailyCount == 1) {
            int secondsUntilMidnight = getSecondsUntilMidnight();
            userAccessCountService.expire(dailyKey, secondsUntilMidnight);
        }

        if (dailyCount > dailyMaxRequests) {
            logger.warn("用户每日访问次数超限: IP={}, 次数={}", ipAddress, dailyCount);
            response.setStatus(403);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("用户行为异常，不能继续访问系统，请联系管理员！");
            return false;
        }

        return true;
    }

    /**
     * 获取客户端IP地址
     * @param request HTTP请求
     * @return IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 从请求URI中提取文件名
     * @param requestUri 请求URI
     * @return 文件名
     */
    private String extractFileName(String requestUri) {
        int lastSlashIndex = requestUri.lastIndexOf('/');
        return lastSlashIndex >= 0 ? requestUri.substring(lastSlashIndex + 1) : requestUri;
    }

    /**
     * 获取距离午夜还有多少秒
     * @return 秒数
     */
    private int getSecondsUntilMidnight() {
        return (int) (java.time.LocalDateTime.now().until(java.time.LocalDate.now().plusDays(1).atStartOfDay(),
                java.time.temporal.ChronoUnit.SECONDS));
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }
}
