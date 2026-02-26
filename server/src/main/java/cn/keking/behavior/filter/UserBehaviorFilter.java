package cn.keking.behavior.filter;

import cn.keking.behavior.config.BehaviorConfig;
import cn.keking.behavior.model.RequestLog;
import cn.keking.behavior.service.AlertMailService;
import cn.keking.behavior.service.RequestLogService;
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
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 用户行为过滤器
 * 拦截预览请求，记录用户行为并检测异常访问
 */
public class UserBehaviorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorFilter.class);

    /**
     * 周期内访问超限提示信息
     */
    private static final String PERIOD_BLOCK_MESSAGE = "请求太频繁，请稍后再试！";

    /**
     * 每日访问超限提示信息
     */
    private static final String DAILY_BLOCK_MESSAGE = "用户行为异常，不能继续访问系统，请联系管理员！";

    /**
     * HTTP状态码：请求过多
     */
    private static final int HTTP_STATUS_TOO_MANY_REQUESTS = 429;

    private RequestLogService requestLogService;
    private AlertMailService alertMailService;

    /**
     * 默认构造函数
     */
    public UserBehaviorFilter() {
    }

    /**
     * 设置请求日志服务
     * @param requestLogService 请求日志服务
     */
    public void setRequestLogService(RequestLogService requestLogService) {
        this.requestLogService = requestLogService;
    }

    /**
     * 设置告警邮件服务
     * @param alertMailService 告警邮件服务
     */
    public void setAlertMailService(AlertMailService alertMailService) {
        this.alertMailService = alertMailService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("UserBehaviorFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!BehaviorConfig.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            String ipAddress = WebUtils.getClientIpAddress(httpRequest);

            if (requestLogService.isDailyThresholdExceeded(ipAddress)) {
                logger.warn("IP {} exceeded daily threshold, blocking request", ipAddress);
                sendBlockResponse(httpResponse, DAILY_BLOCK_MESSAGE);
                return;
            }

            int periodCount = requestLogService.recordAndGetPeriodCount(ipAddress);
            int dailyCount = requestLogService.recordAndGetDailyCount(ipAddress);

            if (periodCount > BehaviorConfig.getPeriodThreshold()) {
                logger.warn("IP {} exceeded period threshold (count: {}), blocking request", ipAddress, periodCount);
                if (requestLogService.shouldSendPeriodAlert(ipAddress)) {
                    alertMailService.sendPeriodAlertAsync(ipAddress, periodCount, BehaviorConfig.getPeriodMinutes());
                }
                sendBlockResponse(httpResponse, PERIOD_BLOCK_MESSAGE);
                return;
            }

            if (dailyCount > BehaviorConfig.getDailyThreshold()) {
                logger.warn("IP {} exceeded daily threshold (count: {}), blocking request", ipAddress, dailyCount);
                if (requestLogService.shouldSendDailyAlert(ipAddress)) {
                    alertMailService.sendDailyAlertAsync(ipAddress, dailyCount);
                }
                sendBlockResponse(httpResponse, DAILY_BLOCK_MESSAGE);
                return;
            }

            String fileName = extractFileName(httpRequest);
            RequestLog requestLog = new RequestLog(ipAddress, fileName, LocalDateTime.now());
            requestLogService.saveRequestLogAsync(requestLog);

        } catch (Exception e) {
            logger.error("Error in UserBehaviorFilter, allowing request to proceed", e);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("UserBehaviorFilter destroyed");
    }

    /**
     * 从请求中提取文件名
     * @param request HTTP请求
     * @return 文件名
     */
    private String extractFileName(HttpServletRequest request) {
        String url = WebUtils.getSourceUrl(request);
        if (StringUtils.hasText(url)) {
            return WebUtils.getFileNameFromURL(url);
        }
        return request.getRequestURI();
    }

    /**
     * 发送拦截响应
     * @param response HTTP响应
     * @param message 提示信息
     * @throws IOException IO异常
     */
    private void sendBlockResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HTTP_STATUS_TOO_MANY_REQUESTS);
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
    }
}
