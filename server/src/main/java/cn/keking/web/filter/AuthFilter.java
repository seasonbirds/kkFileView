package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AuthFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);
    private static final String TOKEN_COOKIE_NAME = "TOKEN";
    private static final String USER_KEY_PREFIX = "USER-";
    private static final String FILE_AUTH_FIELD = "fileAuth";
    private static final String AUTH_PREVIEW = "preview";
    private static final String AUTH_DOWNLOAD = "download";

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String notLoginHtmlView;
    private String loginExpiredHtmlView;
    private String noPermissionHtmlView;

    public AuthFilter(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        loadErrorPage("web/notLogin.html", html -> this.notLoginHtmlView = html);
        loadErrorPage("web/loginExpired.html", html -> this.loginExpiredHtmlView = html);
        loadErrorPage("web/noPermission.html", html -> this.noPermissionHtmlView = html);
    }

    private void loadErrorPage(String path, java.util.function.Consumer<String> setter) {
        ClassPathResource classPathResource = new ClassPathResource(path);
        try {
            classPathResource.getInputStream();
            byte[] bytes = FileCopyUtils.copyToByteArray(classPathResource.getInputStream());
            setter.accept(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            logger.error("Failed to load {} file", path, e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!ConfigConstants.isAuthEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String token = getTokenFromCookie(httpRequest);
        if (token == null || token.isEmpty()) {
            logger.warn("Request without token: {}", httpRequest.getRequestURI());
            writeResponse(httpResponse, notLoginHtmlView);
            return;
        }

        String userKey = USER_KEY_PREFIX + token;
        RBucket<String> userBucket = redissonClient.getBucket(userKey);
        String userInfoJson = userBucket.get();

        if (userInfoJson == null || userInfoJson.isEmpty()) {
            logger.warn("User info not found in redis for token: {}", token);
            writeResponse(httpResponse, loginExpiredHtmlView);
            return;
        }

        if (!hasPreviewPermission(userInfoJson)) {
            logger.warn("User has no preview permission, token: {}", token);
            writeResponse(httpResponse, noPermissionHtmlView);
            return;
        }

        chain.doFilter(request, response);
    }

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

    private boolean hasPreviewPermission(String userInfoJson) {
        try {
            JsonNode jsonNode = objectMapper.readTree(userInfoJson);
            JsonNode fileAuthNode = jsonNode.get(FILE_AUTH_FIELD);
            if (fileAuthNode == null) {
                return false;
            }
            String fileAuth = fileAuthNode.asText("");
            return AUTH_PREVIEW.equals(fileAuth) || AUTH_DOWNLOAD.equals(fileAuth);
        } catch (Exception e) {
            logger.error("Failed to parse user info json", e);
            return false;
        }
    }

    private void writeResponse(HttpServletResponse response, String html) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(html);
        response.getWriter().close();
    }

    @Override
    public void destroy() {
    }
}
