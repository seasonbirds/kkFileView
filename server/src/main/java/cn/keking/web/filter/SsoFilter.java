package cn.keking.web.filter;

import cn.keking.config.SsoConfig;
import cn.keking.utils.WebUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.File;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 单点登录和权限校验过滤器
 * @author AI Assistant
 */
public class SsoFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SsoFilter.class);
    private SsoConfig ssoConfig;
    private ObjectMapper objectMapper;
    private RedissonClient redissonClient;

    @Override
    public void init(FilterConfig filterConfig) {
        // 初始化Redisson客户端
        if (ssoConfig.isEnabled()) {
            Config config = new Config();
            SingleServerConfig singleServerConfig = config.useSingleServer()
                    .setAddress("redis://" + ssoConfig.getRedis().getHost() + ":" + ssoConfig.getRedis().getPort())
                    .setPassword(ssoConfig.getRedis().getPassword())
                    .setDatabase(ssoConfig.getRedis().getDatabase())
                    .setConnectTimeout(ssoConfig.getRedis().getTimeout());
            redissonClient = org.redisson.Redisson.create(config);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 如果未启用单点登录，则直接通过
        if (!ssoConfig.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        // 1. 获取用户token
        String token = getTokenFromCookie(httpRequest);
        if (!StringUtils.hasText(token)) {
            logger.warn("请求没有token信息");
            sendErrorResponse(httpResponse, "请先登录系统");
            return;
        }

        // 2. 从Redis中获取用户信息
        String redisKey = ssoConfig.getRedis().getUserPrefix() + token;
        String userInfoJson = null;
        try {
            RBucket<String> bucket = redissonClient.getBucket(redisKey);
            userInfoJson = bucket.get();
        } catch (Exception e) {
            logger.error("从Redis获取用户信息失败", e);
            sendErrorResponse(httpResponse, "系统异常，请稍后重试");
            return;
        }

        if (!StringUtils.hasText(userInfoJson)) {
            logger.warn("redis中不存在该用户信息，token: {}", token);
            sendErrorResponse(httpResponse, "登录状态已失效，请重新登录");
            return;
        }

        // 3. 解析用户信息并检查权限
        try {
            Map<String, Object> userInfo = objectMapper.readValue(userInfoJson, Map.class);
            String fileAuth = (String) userInfo.get("fileAuth");

            if (!StringUtils.hasText(fileAuth) || fileAuth.equals("")) {
                logger.warn("用户没有预览权限，token: {}", token);
                sendErrorResponse(httpResponse, "无相关权限，请联系管理员");
                return;
            }

            if (!fileAuth.equals("preview") && !fileAuth.equals("download")) {
                logger.warn("用户权限无效，token: {}, fileAuth: {}", token, fileAuth);
                sendErrorResponse(httpResponse, "无相关权限，请联系管理员");
                return;
            }

            // 4. 权限验证通过，继续请求
            chain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("解析用户信息失败", e);
            sendErrorResponse(httpResponse, "系统异常，请稍后重试");
        }
    }

    /**
     * 从Cookie中获取token
     */
    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ssoConfig.getTokenName().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse response, String errorMsg) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // 使用freemarker模板渲染错误页面
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setDirectoryForTemplateLoading(new File("server/src/main/resources/web"));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        Template template = cfg.getTemplate("fileNotSupported.ftl");
        Map<String, Object> data = new HashMap<>();
        data.put("fileType", "单点登录");
        data.put("msg", errorMsg);
        template.process(data, response.getWriter());
    }

    public void setSsoConfig(SsoConfig ssoConfig) {
        this.ssoConfig = ssoConfig;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void destroy() {
        // 关闭Redisson客户端
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }
}
