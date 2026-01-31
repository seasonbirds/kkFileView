package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import cn.keking.service.AccessCountCacheService;
import cn.keking.service.AlertEmailService;
import cn.keking.service.UserBehaviorService;
import cn.keking.utils.WebUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户行为监控和访问频率限制过滤器
 */
public class UserBehaviorFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(UserBehaviorFilter.class);
    
    // 用于记录IP被封禁的时间，key为IP地址，value为解封时间
    private final ConcurrentHashMap<String, LocalDateTime> blockedIps = new ConcurrentHashMap<>();

    @Autowired
    private UserBehaviorService userBehaviorService;
    
    @Autowired
    private AlertEmailService alertEmailService;
    
    @Autowired
    private AccessCountCacheService accessCountCacheService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 如果用户行为监控功能未启用，直接放行
        if (!ConfigConstants.isUserBehaviorMonitorEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        
        String ipAddress = WebUtils.getIpAddress(httpRequest);
        String requestUrl = WebUtils.getSourceUrl(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        try {
            // 检查IP是否被封禁
            if (isIpBlocked(ipAddress)) {
                logger.warn("IP {} 已被封禁，拒绝访问", ipAddress);
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.getWriter().write("用户行为异常，不能继续访问系统，请联系管理员！");
                httpResponse.getWriter().close();
                return;
            }
            
            // 异步记录用户行为
            userBehaviorService.recordUserBehavior(ipAddress, requestUrl, userAgent);
            
            // 更新访问计数
            if (accessCountCacheService.isRedisAvailable()) {
                // 使用Redis缓存
                accessCountCacheService.incrementTimeWindowCount(ipAddress, ConfigConstants.getTimeWindowMinutes());
                accessCountCacheService.incrementDailyCount(ipAddress);
                
                // 检查每日访问次数是否超限
                long dailyAccessCount = accessCountCacheService.getDailyCount(ipAddress);
                if (dailyAccessCount > ConfigConstants.getDailyAccessThreshold()) {
                    // 封禁IP到当天24点
                    LocalDateTime unblockTime = LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
                    blockedIps.put(ipAddress, unblockTime);
                    
                    // 异步标记行为为异常
                    userBehaviorService.markBehaviorAsAbnormal(ipAddress, "每日访问次数超限");
                    
                    // 异步发送告警邮件
                    alertEmailService.sendDailyAccessExceededAlert(ipAddress, (int)dailyAccessCount);
                    
                    logger.warn("IP {} 今日访问次数 {} 超过阈值 {}，已封禁至当天24点", 
                            ipAddress, dailyAccessCount, ConfigConstants.getDailyAccessThreshold());
                    
                    httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    httpResponse.getWriter().write("用户行为异常，不能继续访问系统，请联系管理员！");
                    httpResponse.getWriter().close();
                    return;
                }
                
                // 检查时间窗口内访问次数是否超限
                long timeWindowAccessCount = accessCountCacheService.getTimeWindowCount(ipAddress);
                if (timeWindowAccessCount > ConfigConstants.getTimeWindowThreshold()) {
                    // 异步标记行为为异常
                    userBehaviorService.markBehaviorAsAbnormal(ipAddress, "时间窗口内访问频率过高");
                    
                    // 异步发送告警邮件
                    alertEmailService.sendAbnormalBehaviorAlert(ipAddress, 
                            ConfigConstants.getTimeWindowMinutes(), (int)timeWindowAccessCount);
                    
                    logger.warn("IP {} 在过去 {} 分钟内访问了 {} 次，超过阈值 {}，拒绝访问", 
                            ipAddress, ConfigConstants.getTimeWindowMinutes(), 
                            timeWindowAccessCount, ConfigConstants.getTimeWindowThreshold());
                    
                    httpResponse.setStatus(429); // 429 Too Many Requests
                    httpResponse.getWriter().write("请求太频繁，请稍后再试！");
                    httpResponse.getWriter().close();
                    return;
                }
            } else {
                // Redis不可用，回退到数据库查询方式
                // 检查每日访问次数是否超限
                if (userBehaviorService.isDailyAccessExceeded(ipAddress, ConfigConstants.getDailyAccessThreshold())) {
                    // 封禁IP到当天24点
                    LocalDateTime unblockTime = LocalDateTime.now().toLocalDate().atTime(LocalTime.MAX);
                    blockedIps.put(ipAddress, unblockTime);
                    
                    // 异步标记行为为异常
                    userBehaviorService.markBehaviorAsAbnormal(ipAddress, "每日访问次数超限");
                    
                    // 异步发送告警邮件
                    alertEmailService.sendDailyAccessExceededAlert(ipAddress, 0); // 无法获取准确计数
                    
                    logger.warn("IP {} 每日访问次数超过阈值 {}，已封禁至当天24点", 
                            ipAddress, ConfigConstants.getDailyAccessThreshold());
                    
                    httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    httpResponse.getWriter().write("用户行为异常，不能继续访问系统，请联系管理员！");
                    httpResponse.getWriter().close();
                    return;
                }
                
                // 检查时间窗口内访问次数是否超限
                if (userBehaviorService.isAccessExceeded(ipAddress, 
                        ConfigConstants.getTimeWindowMinutes(), 
                        ConfigConstants.getTimeWindowThreshold())) {
                    
                    // 异步标记行为为异常
                    userBehaviorService.markBehaviorAsAbnormal(ipAddress, "时间窗口内访问频率过高");
                    
                    // 异步发送告警邮件
                    alertEmailService.sendAbnormalBehaviorAlert(ipAddress, 
                            ConfigConstants.getTimeWindowMinutes(), 0); // 无法获取准确计数
                    
                    logger.warn("IP {} 在过去 {} 分钟内访问次数超过阈值 {}，拒绝访问", 
                            ipAddress, ConfigConstants.getTimeWindowMinutes(), 
                            ConfigConstants.getTimeWindowThreshold());
                    
                    httpResponse.setStatus(429); // 429 Too Many Requests
                    httpResponse.getWriter().write("请求太频繁，请稍后再试！");
                    httpResponse.getWriter().close();
                    return;
                }
            }
            
            // 正常访问，继续处理请求
            chain.doFilter(request, response);
        } catch (Exception e) {
            // 发生异常时记录日志，但不影响用户正常访问
            logger.error("用户行为监控处理过程中发生异常", e);
            chain.doFilter(request, response);
        }
    }
    
    /**
     * 检查IP是否被封禁
     */
    private boolean isIpBlocked(String ipAddress) {
        try {
            LocalDateTime unblockTime = blockedIps.get(ipAddress);
            if (unblockTime == null) {
                return false;
            }
            
            // 如果当前时间已超过解封时间，则移除封禁记录
            if (LocalDateTime.now().isAfter(unblockTime)) {
                blockedIps.remove(ipAddress);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            logger.error("检查IP封禁状态时发生异常", e);
            return false; // 出错时不阻止用户访问
        }
    }
}