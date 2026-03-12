package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import cn.keking.service.monitor.AccessCounter;
import cn.keking.service.monitor.UserBehaviorService;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 访问控制过滤器
 * 用于无侵入地记录用户行为并实现访问频率控制
 * 核心设计原则：过滤器出错不影响正常访问
 */
public class AccessControlFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AccessControlFilter.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 封禁页面HTML模板
     */
    private String blockPageHtmlView;

    /**
     * 用户行为服务
     */
    private UserBehaviorService userBehaviorService;

    /**
     * 访问计数器
     */
    private AccessCounter accessCounter;

    @Override
    public void init(FilterConfig filterConfig) {
        try {
            // 加载封禁页面模板
            ClassPathResource classPathResource = new ClassPathResource("web/blockedIp.html");
            byte[] bytes = FileCopyUtils.copyToByteArray(classPathResource.getInputStream());
            this.blockPageHtmlView = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("加载封禁页面模板失败", e);
            this.blockPageHtmlView = "<html><body><h1>访问被拒绝</h1><p>您的IP访问频率过高，请稍后再试</p></body></html>";
        }

        try {
            // 从Spring容器中获取Bean
            userBehaviorService = WebApplicationContextUtils
                    .getRequiredWebApplicationContext(filterConfig.getServletContext())
                    .getBean(UserBehaviorService.class);
            accessCounter = WebApplicationContextUtils
                    .getRequiredWebApplicationContext(filterConfig.getServletContext())
                    .getBean(AccessCounter.class);
        } catch (Exception e) {
            logger.error("初始化访问控制过滤器失败，监控功能将不可用", e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 如果监控未启用或初始化失败，直接放行
        if (!ConfigConstants.getMonitorEnabled() || userBehaviorService == null || accessCounter == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String ipAddress = WebUtils.getIpAddress(httpRequest);

            // 1. 先检查IP是否已被封禁（内存检查，快速拒绝）
            AccessCounter.BlockInfo blockInfo = accessCounter.getBlockInfo(ipAddress);
            if (blockInfo != null) {
                logger.debug("IP {} 已被封禁，拒绝访问，原因: {}", ipAddress, blockInfo.getReason());
                handleBlockedRequest(response, ipAddress);
                return;
            }

            // 2. 增加计数并检查是否超限（原子操作）
            AccessCounter.LimitCheckResult limitResult = accessCounter.incrementAndCheck(ipAddress);

            // 提前获取请求信息
            String requestUrl = httpRequest.getRequestURL().toString();
            String userAgent = httpRequest.getHeader("User-Agent");
            String sourceUrl = WebUtils.getSourceUrl(request);
            String fileName = sourceUrl != null ? WebUtils.getFileNameFromURL(sourceUrl) : null;

            // 3. 如果超限，触发封禁
            if (limitResult.isBlocked()) {
                logger.warn("IP {} 访问超限，触发封禁: {}", ipAddress, limitResult.getBlockReason());
                userBehaviorService.blockIp(ipAddress, limitResult.getBlockReason(), limitResult.isOverDailyLimit());
                handleBlockedRequest(response, ipAddress);

                // 只在首次触发封禁时记录一次（异步）
                userBehaviorService.recordUserBehavior(ipAddress, fileName, requestUrl, userAgent, true);
                return;
            }

            // 4. 正常访问：异步记录用户行为
            userBehaviorService.recordUserBehavior(ipAddress, fileName, requestUrl, userAgent, false);

        } catch (Exception e) {
            // 关键：过滤器出错时不能影响正常访问，记录错误后放行
            logger.error("访问控制过滤器处理异常，放行请求", e);
        }

        // 正常放行
        chain.doFilter(request, response);
    }

    /**
     * 处理被封禁的请求，返回封禁页面
     */
    private void handleBlockedRequest(ServletResponse response, String ipAddress) throws IOException {
        try {
            String html = this.blockPageHtmlView;
            html = html.replace("${ip_address}", ipAddress);

            // 直接从内存缓存获取封禁信息（性能最优）
            AccessCounter.BlockInfo blockInfo = accessCounter.getBlockInfo(ipAddress);
            if (blockInfo != null) {
                html = html.replace("${block_reason}", blockInfo.getReason());
                html = html.replace("${block_end_time}", blockInfo.getBlockEndTime().format(FORMATTER));
            } else {
                html = html.replace("${block_reason}", "访问频率超限");
                html = html.replace("${block_end_time}", "系统自动判定");
            }

            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(html);
        } catch (Exception e) {
            logger.error("返回封禁页面失败", e);
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("<html><body><h1>访问被拒绝</h1></body></html>");
        }
    }

    @Override
    public void destroy() {
        // 清理资源
    }
}
