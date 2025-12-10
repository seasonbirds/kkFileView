package cn.keking.web.filter;

import cn.keking.utils.IpWhitelistUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 源文件下载IP白名单过滤器
 * 仅对/downloadSourceFile接口生效
 * 
 * @author: chenjh
 * @since: 2024/12/10
 */
@Component
public class SourceFileDownloadIpFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceFileDownloadIpFilter.class);
    
    @Value("${download.source.ip.whitelist:*}")
    private String ipWhitelistConfig;
    
    private IpWhitelistUtils ipWhitelistUtils;
    
    public SourceFileDownloadIpFilter(IpWhitelistUtils ipWhitelistUtils) {
        this.ipWhitelistUtils = ipWhitelistUtils;
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.info("源文件下载IP白名单过滤器初始化完成，配置: {}", ipWhitelistConfig);
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 获取客户端IP地址
        String clientIp = ipWhitelistUtils.getClientIpAddress(httpRequest);
        LOGGER.debug("源文件下载请求，客户端IP: {}", clientIp);
        
        // 检查IP白名单
        if (!isIpAllowed(clientIp)) {
            LOGGER.warn("源文件下载请求被拒绝，IP不在白名单中: {}", clientIp);
            httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write("{\"msg\":\"您的IP地址没有下载权限\",\"code\":403}");
            return;
        }
        
        LOGGER.debug("源文件下载请求通过IP白名单验证，IP: {}", clientIp);
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
        LOGGER.info("源文件下载IP白名单过滤器销毁");
    }
    
    /**
     * 检查IP是否在白名单中
     * 
     * @param ipAddress IP地址
     * @return 是否允许访问
     */
    private boolean isIpAllowed(String ipAddress) {
        // 如果配置为*，允许所有IP
        if ("*".equals(ipWhitelistConfig)) {
            return true;
        }
        
        // 使用IpWhitelistUtils进行IP白名单验证
        return ipWhitelistUtils.isIpAllowed(ipAddress);
    }
}