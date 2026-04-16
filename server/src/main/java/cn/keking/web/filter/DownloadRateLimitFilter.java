package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import cn.keking.config.DownloadRedisConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DownloadRateLimitFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(DownloadRateLimitFilter.class);

    private static final String DOWNLOAD_LOCK_PREFIX = "download:lock:";
    private static final String DOWNLOAD_IP_SET = "download:active:ips";
    private static final String DOWNLOAD_IP_FILE_MAP = "download:ip:file";
    private static final String RATE_LIMIT_ERROR_MSG = "请求太频繁，请稍后再试";

    private RedissonClient redissonClient;
    private DownloadRedisConfig downloadRedisConfig;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
        if (context != null) {
            try {
                redissonClient = context.getBean("downloadRedissonClient", RedissonClient.class);
                downloadRedisConfig = context.getBean(DownloadRedisConfig.class);
                logger.info("DownloadRateLimitFilter initialized with Redis");
            } catch (Exception e) {
                logger.warn("Download Redis client not available, rate limiting disabled: {}", e.getMessage());
            }
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIp(httpRequest);
        String url = httpRequest.getRequestURI();
        String fileUrl = httpRequest.getParameter("url");

        logger.info("Download request from IP: {}, URL: {}", clientIp, url);

        if (ConfigConstants.getDownloadSourceFileEnabled() == null || !ConfigConstants.getDownloadSourceFileEnabled()) {
            writeErrorResponse(httpResponse, HttpServletResponse.SC_FORBIDDEN, "下载功能已禁用");
            return;
        }

        if (redissonClient == null || downloadRedisConfig == null || !downloadRedisConfig.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        Integer maxIp = ConfigConstants.getDownloadRateLimitMaxIp();
        if (maxIp == null) {
            maxIp = 10;
        }

        Integer timeout = ConfigConstants.getDownloadRateLimitTimeout();
        if (timeout == null) {
            timeout = 300;
        }

        RSet<String> activeIps = redissonClient.getSet(DOWNLOAD_IP_SET);
        RMap<String, String> ipFileMap = redissonClient.getMap(DOWNLOAD_IP_FILE_MAP);

        String currentFileKey = clientIp + ":" + (fileUrl != null ? fileUrl : "unknown");
        String lockKey = DOWNLOAD_LOCK_PREFIX + clientIp;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(5, timeout, TimeUnit.SECONDS);
            if (!acquired) {
                logger.warn("IP {} failed to acquire download lock", clientIp);
                writeErrorResponse(httpResponse, HttpServletResponse.SC_TOO_MANY_REQUESTS, RATE_LIMIT_ERROR_MSG);
                return;
            }

            try {
                String ipCurrentFile = ipFileMap.get(clientIp);
                if (ipCurrentFile != null && !ipCurrentFile.isEmpty()) {
                    if (!ipCurrentFile.equals(currentFileKey)) {
                        logger.warn("IP {} is already downloading another file: {}", clientIp, ipCurrentFile);
                        writeErrorResponse(httpResponse, HttpServletResponse.SC_TOO_MANY_REQUESTS, RATE_LIMIT_ERROR_MSG);
                        return;
                    }
                }

                int activeCount = activeIps.size();
                if (activeCount >= maxIp && !activeIps.contains(clientIp)) {
                    logger.warn("Too many active downloads, active: {}, max: {}", activeCount, maxIp);
                    writeErrorResponse(httpResponse, HttpServletResponse.SC_TOO_MANY_REQUESTS, RATE_LIMIT_ERROR_MSG);
                    return;
                }

                activeIps.add(clientIp);
                ipFileMap.put(clientIp, currentFileKey);

                activeIps.expire(timeout, TimeUnit.SECONDS);
                ipFileMap.expire(timeout, TimeUnit.SECONDS);

                logger.info("IP {} started downloading, active IPs: {}", clientIp, activeIps.size());

                try {
                    filterChain.doFilter(request, response);
                } finally {
                    activeIps.remove(clientIp);
                    ipFileMap.remove(clientIp);
                    logger.info("IP {} finished downloading, active IPs: {}", clientIp, activeIps.size());
                }

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            logger.error("Download rate limit interrupted", e);
            Thread.currentThread().interrupt();
            writeErrorResponse(httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误");
        } catch (Exception e) {
            logger.error("Download rate limit error", e);
            writeErrorResponse(httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误");
        }
    }

    private String getClientIp(HttpServletRequest request) {
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
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", status);
        result.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    @Override
    public void destroy() {
    }
}
