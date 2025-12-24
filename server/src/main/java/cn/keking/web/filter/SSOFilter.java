package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * SSO过滤器，用于单点登录和权限校验
 * @author AI Assistant
 * @since 2025/12/24
 */
public class SSOFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SSOFilter.class);
    private static final String TOKEN_COOKIE_NAME = "TOKEN";
    private static final String USER_REDIS_PREFIX = "USER-";
    private static final String FILE_AUTH_FIELD = "fileAuth";
    private static final String PREVIEW_PERMISSION = "preview";
    private static final String DOWNLOAD_PERMISSION = "download";

    private RedissonClient redissonClient;
    private ObjectMapper objectMapper;
    private Configuration freemarkerConfig;
    private String loginRequiredTemplate;
    private String sessionExpiredTemplate;
    private String noPermissionTemplate;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            // 初始化ObjectMapper
            this.objectMapper = new ObjectMapper();

            // 初始化Freemarker配置
            this.freemarkerConfig = new Configuration(Configuration.VERSION_2_3_31);
            this.freemarkerConfig.setClassForTemplateLoading(getClass(), "/web/");
            this.freemarkerConfig.setDefaultEncoding("UTF-8");

            // 设置模板路径
            this.loginRequiredTemplate = "loginRequired.ftl";
            this.sessionExpiredTemplate = "sessionExpired.ftl";
            this.noPermissionTemplate = "noPermission.ftl";
        } catch (Exception e) {
            logger.error("Failed to initialize SSOFilter", e);
            throw new ServletException("Failed to initialize SSOFilter", e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 从Cookie中获取TOKEN
        String token = getTokenFromCookie(httpRequest);

        // 1. 检查是否有TOKEN
        if (token == null) {
            logger.warn("No token found in request, access denied");
            sendErrorResponse(httpResponse, loginRequiredTemplate, "请先登录系统");
            return;
        }

        // 2. 检查Redis中是否存在用户信息
        String redisKey = USER_REDIS_PREFIX + token;
        String userJson = null;
        try {
            userJson = redissonClient.<String>getBucket(redisKey).get();
        } catch (Exception e) {
            logger.error("Failed to get user information from Redis", e);
            sendErrorResponse(httpResponse, sessionExpiredTemplate, "登录状态已失效，请重新登录");
            return;
        }

        if (userJson == null) {
            logger.warn("User information not found in Redis, token: {}", token);
            sendErrorResponse(httpResponse, sessionExpiredTemplate, "登录状态已失效，请重新登录");
            return;
        }

        // 3. 检查用户是否有预览权限
        try {
            JsonNode userNode = objectMapper.readTree(userJson);
            String fileAuth = userNode.path(FILE_AUTH_FIELD).asText();

            if (fileAuth == null || fileAuth.isEmpty()) {
                logger.warn("User has no file preview permission, token: {}", token);
                sendErrorResponse(httpResponse, noPermissionTemplate, "无相关权限，请联系管理员");
                return;
            }

            if (!fileAuth.equals(PREVIEW_PERMISSION) && !fileAuth.equals(DOWNLOAD_PERMISSION)) {
                logger.warn("User has invalid file permission: {}, token: {}", fileAuth, token);
                sendErrorResponse(httpResponse, noPermissionTemplate, "无相关权限，请联系管理员");
                return;
            }

            // 权限校验通过，继续请求
            chain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Failed to parse user information JSON: {}", userJson, e);
            sendErrorResponse(httpResponse, sessionExpiredTemplate, "登录状态已失效，请重新登录");
        }
    }

    @Override
    public void destroy() {
        // 清理资源
    }

    /**
     * 从Cookie中获取TOKEN
     * @param request HttpServletRequest
     * @return TOKEN值或null
     */
    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 发送错误响应
     * @param response HttpServletResponse
     * @param templateName 模板名称
     * @param message 错误消息
     * @throws IOException IO异常
     */
    private void sendErrorResponse(HttpServletResponse response, String templateName, String message) throws IOException {
        try {
            response.setContentType("text/html;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            // 创建模板数据
            Map<String, Object> templateData = new HashMap<>();
            templateData.put("message", message);

            // 加载模板
            Template template = freemarkerConfig.getTemplate(templateName);

            // 渲染模板
            StringWriter writer = new StringWriter();
            template.process(templateData, writer);
            String html = writer.toString();

            // 发送响应
            response.getWriter().write(html);
            response.getWriter().close();
        } catch (TemplateException e) {
            logger.error("Failed to process Freemarker template: {}", templateName, e);
            throw new IOException("Failed to process template", e);
        }
    }

    // 依赖注入
    public void setRedissonClient(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}