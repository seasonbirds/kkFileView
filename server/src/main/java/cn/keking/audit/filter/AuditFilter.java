package cn.keking.audit.filter;

import cn.keking.audit.config.AuditConfig;
import cn.keking.audit.service.UserBehaviorService;
import cn.keking.utils.WebUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;

@Component
public class AuditFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuditFilter.class);

    private final UserBehaviorService userBehaviorService;

    public AuditFilter(UserBehaviorService userBehaviorService) {
        this.userBehaviorService = userBehaviorService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("AuditFilter initialized, audit enabled: {}", AuditConfig.isAuditEnabled());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        if (!AuditConfig.isAuditEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String ip = getClientIp(httpRequest);
        String requestUri = httpRequest.getRequestURI();
        String requestMethod = httpRequest.getMethod();

        UserBehaviorService.AccessCheckResult result = userBehaviorService.checkAccess(ip);

        if (!result.isAllowed()) {
            logger.warn("Access denied for IP: {}, reason: {}", ip, result.getRejectMessage());
            sendRejectResponse(httpResponse, result.getRejectMessage());
            return;
        }

        String fileName = extractFileName(httpRequest);

        userBehaviorService.recordUserBehaviorAsync(ip, fileName, requestUri, requestMethod);

        chain.doFilter(request, response);
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
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }

    private String extractFileName(HttpServletRequest request) {
        try {
            String sourceUrl = WebUtils.getSourceUrl(request);
            if (sourceUrl != null && !sourceUrl.isEmpty()) {
                return WebUtils.getFileNameFromURL(sourceUrl);
            }
        } catch (Exception e) {
            logger.debug("Failed to extract file name from request", e);
        }
        return null;
    }

    private void sendRejectResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset=\"UTF-8\">");
        out.println("<title>访问受限</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }");
        out.println(".message { font-size: 24px; color: #d9534f; margin-top: 20px; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<div class=\"message\">" + message + "</div>");
        out.println("</body>");
        out.println("</html>");
        out.flush();
    }

    @Override
    public void destroy() {
        logger.info("AuditFilter destroyed");
    }
}
