package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import cn.keking.utils.WebUtils;
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

/**
 * 下载源文件限流过滤器
 *
 * 限流逻辑：
 * 1. 同一个IP地址在一个文件没下载完成前，不能下载其他文件
 * 2. 同时只能允许m个不同IP地址下载文件，m可配置
 *
 * 基于Redis实现，支持集群部署。如果Redis不可用，则限流不生效。
 *
 * @author keking
 */
public class DownloadRateLimitFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(DownloadRateLimitFilter.class);

    /**
     * Redis分布式锁前缀，用于保证同一IP并发操作的原子性
     * 完整Key格式：download:lock:{clientIp}
     */
    private static final String DOWNLOAD_LOCK_PREFIX = "download:lock:";

    /**
     * Redis Set存储当前正在下载文件的IP集合
     * 用于控制同时允许的最大不同IP下载数量
     */
    private static final String DOWNLOAD_IP_SET = "download:active:ips";

    /**
     * Redis Map存储IP与正在下载文件的映射关系
     * 用于检查同一个IP是否正在下载其他文件
     * Key格式：{clientIp}，Value格式：{clientIp}:{fileUrl}
     */
    private static final String DOWNLOAD_IP_FILE_MAP = "download:ip:file";

    /**
     * 限流错误提示信息
     */
    private static final String RATE_LIMIT_ERROR_MSG = "请求太频繁，请稍后再试";

    /**
     * Redisson客户端，用于Redis操作
     * 在init方法中从Spring容器获取
     */
    private RedissonClient redissonClient;

    /**
     * JSON序列化工具，用于错误响应输出
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 过滤器初始化方法
     *
     * 从Spring容器中获取Redisson客户端实例。
     * 如果获取失败（Redis未配置或不可用），则限流功能不生效。
     *
     * @param filterConfig 过滤器配置
     * @throws ServletException Servlet异常
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());
        if (context != null) {
            try {
                redissonClient = context.getBean("downloadRedissonClient", RedissonClient.class);
                logger.info("DownloadRateLimitFilter initialized with Redis");
            } catch (Exception e) {
                logger.warn("Download Redis client not available, rate limiting disabled: {}", e.getMessage());
            }
        }
    }

    /**
     * 过滤器核心拦截方法
     *
     * 执行以下限流逻辑：
     * 1. 检查Redis是否可用，不可用则直接放行
     * 2. 尝试获取分布式锁，保证同一IP的操作原子性
     * 3. 检查同一IP是否正在下载其他文件，如果是则拒绝
     * 4. 检查当前活跃IP数是否超过配置的最大值，如果是则拒绝
     * 5. 通过所有检查后，将IP加入活跃集合，记录正在下载的文件
     * 6. 执行请求，请求完成后清理Redis中的记录
     *
     * @param request  Servlet请求
     * @param response Servlet响应
     * @param filterChain 过滤器链
     * @throws IOException      IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 获取客户端真实IP地址（支持多级代理）
        String clientIp = WebUtils.getClientIp(httpRequest);

        // Redis未初始化或不可用时，直接放行（限流不生效）
        if (redissonClient == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从配置中获取限流参数，使用默认值兜底
        int maxIp = getOrDefault(ConfigConstants.getDownloadRateLimitMaxIp(), 10);
        int timeout = getOrDefault(ConfigConstants.getDownloadRateLimitTimeout(), 300);

        // 获取请求的文件URL参数，构建文件Key（用于判断是否正在下载同一个文件）
        String fileUrl = httpRequest.getParameter("url");
        String currentFileKey = clientIp + ":" + (fileUrl != null ? fileUrl : "unknown");

        // 获取Redis数据结构
        RSet<String> activeIps = redissonClient.getSet(DOWNLOAD_IP_SET);
        RMap<String, String> ipFileMap = redissonClient.getMap(DOWNLOAD_IP_FILE_MAP);

        // 基于IP的分布式锁，保证同一IP并发操作的原子性
        RLock lock = redissonClient.getLock(DOWNLOAD_LOCK_PREFIX + clientIp);

        try {
            // 尝试获取锁，等待5秒，持有锁最长timeout秒
            boolean acquired = lock.tryLock(5, timeout, TimeUnit.SECONDS);
            if (!acquired) {
                // 锁获取失败，说明该IP正在执行其他下载操作
                logger.warn("IP {} failed to acquire download lock", clientIp);
                writeErrorResponse(httpResponse, 429, RATE_LIMIT_ERROR_MSG);
                return;
            }

            try {
                // 限流规则1：检查同一IP是否正在下载其他文件
                String ipCurrentFile = ipFileMap.get(clientIp);
                if (ipCurrentFile != null && !ipCurrentFile.isEmpty() && !ipCurrentFile.equals(currentFileKey)) {
                    logger.warn("IP {} is already downloading another file: {}", clientIp, ipCurrentFile);
                    writeErrorResponse(httpResponse, 429, RATE_LIMIT_ERROR_MSG);
                    return;
                }

                // 限流规则2：检查当前活跃IP数是否超过配置的最大值
                int activeCount = activeIps.size();
                if (activeCount >= maxIp && !activeIps.contains(clientIp)) {
                    logger.warn("Too many active downloads, active: {}, max: {}", activeCount, maxIp);
                    writeErrorResponse(httpResponse, 429, RATE_LIMIT_ERROR_MSG);
                    return;
                }

                // 记录当前IP为活跃状态，并记录正在下载的文件
                activeIps.add(clientIp);
                ipFileMap.put(clientIp, currentFileKey);

                // 设置Redis数据的过期时间，防止异常情况导致数据不释放
                activeIps.expire(timeout, TimeUnit.SECONDS);
                ipFileMap.expire(timeout, TimeUnit.SECONDS);

                logger.info("IP {} started downloading, active IPs: {}", clientIp, activeIps.size());

                // 执行实际的下载请求
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    // 请求完成后，清理Redis中的记录
                    activeIps.remove(clientIp);
                    ipFileMap.remove(clientIp);
                    logger.info("IP {} finished downloading, active IPs: {}", clientIp, activeIps.size());
                }

            } finally {
                // 释放分布式锁（确保当前线程持有该锁）
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            // 线程被中断，恢复中断状态并返回错误
            logger.error("Download rate limit interrupted", e);
            Thread.currentThread().interrupt();
            writeErrorResponse(httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误");
        } catch (Exception e) {
            // 其他异常，返回错误响应
            logger.error("Download rate limit error", e);
            writeErrorResponse(httpResponse, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "服务器内部错误");
        }
    }

    /**
     * 空值处理工具方法
     *
     * @param value        原始值（可能为null）
     * @param defaultValue 默认值
     * @return 如果原始值不为null则返回原始值，否则返回默认值
     */
    private int getOrDefault(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    /**
     * 输出JSON格式的错误响应
     *
     * @param response HTTP响应对象
     * @param status   HTTP状态码
     * @param message  错误信息
     * @throws IOException IO异常
     */
    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", status);
        result.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    /**
     * 过滤器销毁方法
     */
    @Override
    public void destroy() {
    }
}
