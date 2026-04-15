package cn.keking.audit.filter;

import cn.keking.audit.service.UserBehaviorService;
import cn.keking.utils.WebUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import freemarker.template.Template;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户行为审计Filter
 * 用于拦截预览请求，记录用户行为并进行访问控制
 */
public class AuditFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuditFilter.class);

    private final UserBehaviorService userBehaviorService;
    private final FreeMarkerConfigurer freeMarkerConfigurer;

    public AuditFilter(UserBehaviorService userBehaviorService, FreeMarkerConfigurer freeMarkerConfigurer) {
        this.userBehaviorService = userBehaviorService;
        this.freeMarkerConfigurer = freeMarkerConfigurer;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("AuditFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String ip = WebUtils.getClientIp(httpRequest);
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

    /**
     * 从请求中提取文件名
     *
     * @param request HTTP请求对象
     * @return 文件名，如果无法提取则返回null
     */
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

    /**
     * 发送拒绝访问响应
     * 使用freemarker模板渲染页面
     *
     * @param response HTTP响应对象
     * @param message 拒绝消息
     * @throws IOException IO异常
     */
    private void sendRejectResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        try {
            Template template = freeMarkerConfigurer.getConfiguration().getTemplate("accessDenied.ftl");
            Map<String, Object> model = new HashMap<>();
            model.put("msg", message);

            StringWriter writer = new StringWriter();
            template.process(model, writer);

            response.getWriter().write(writer.toString());
            response.getWriter().flush();
        } catch (Exception e) {
            logger.error("Failed to render access denied template", e);
            response.getWriter().write("<html><body><h1>" + message + "</h1></body></html>");
            response.getWriter().flush();
        }
    }

    @Override
    public void destroy() {
        logger.info("AuditFilter destroyed");
    }
}
