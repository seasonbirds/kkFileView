package cn.keking.web.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * SSO单点登录过滤器
 * 实现token验证、用户信息获取和权限检查
 * @author AI Assistant
 * @since 2023/10/25
 */
@Component
public class SSOFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SSOFilter.class);
    private static final String TOKEN_COOKIE_NAME = "TOKEN";
    private static final String REDIS_USER_KEY_PREFIX = "USER-";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    @Qualifier("ssoRedissonClient")
    private RedissonClient redissonClient;
    private Configuration freemarkerConfig;

    @Override
    public void init(FilterConfig filterConfig) {
        // 初始化Freemarker配置
        freemarkerConfig = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        freemarkerConfig.setClassForTemplateLoading(SSOFilter.class, "/web/");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. 获取token
        String token = getTokenFromCookie(httpRequest);
        if (token == null) {
            logger.warn("No token found in request");
            renderTemplate(httpResponse, "loginRequired.ftl");
            return;
        }

        // 2. 从Redis获取用户信息
        String redisKey = REDIS_USER_KEY_PREFIX + token;
        String userInfoStr = null;
        try {
            userInfoStr = redissonClient.getBucket(redisKey).get();
        } catch (Exception e) {
            logger.error("Failed to get user info from Redis", e);
            renderTemplate(httpResponse, "sessionExpired.ftl");
            return;
        }

        if (userInfoStr == null) {
            logger.warn("User info not found in Redis for token: {}", token);
            renderTemplate(httpResponse, "sessionExpired.ftl");
            return;
        }

        // 3. 解析用户信息并检查权限
        try {
            JsonNode userInfo = objectMapper.readTree(userInfoStr);
            String fileAuth = userInfo.has("fileAuth") ? userInfo.get("fileAuth").asText() : "";
            
            // 检查是否有预览权限
            if ("preview".equals(fileAuth) || "download".equals(fileAuth) || "".equals(fileAuth)) {
                // 有权限，继续处理
                chain.doFilter(request, response);
            } else {
                logger.warn("User has no permission to preview files, fileAuth: {}", fileAuth);
                renderTemplate(httpResponse, "noPermission.ftl");
            }
        } catch (Exception e) {
            logger.error("Failed to parse user info", e);
            renderTemplate(httpResponse, "sessionExpired.ftl");
        }
    }

    /**
     * 从Cookie中获取token
     */
    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 渲染Freemarker模板
     */
    private void renderTemplate(HttpServletResponse response, String templateName)
            throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        Template template = freemarkerConfig.getTemplate(templateName);
        Map<String, Object> dataModel = new HashMap<>();
        StringWriter writer = new StringWriter();
        template.process(dataModel, writer);
        response.getWriter().write(writer.toString());
        response.getWriter().close();
    }

    @Override
    public void destroy() {
        // 清理资源
    }

    // Setter方法，用于Spring注入
    public void setRedissonClient(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }
}
