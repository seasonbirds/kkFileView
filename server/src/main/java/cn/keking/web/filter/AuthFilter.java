package cn.keking.web.filter;

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

/**
 * 单点登录认证过滤器
 * 实现基于Cookie Token的用户认证和权限校验
 *
 * <p>认证逻辑：
 * <ol>
 *     <li>从Cookie中获取用户Token（名称为：TOKEN）</li>
 *     <li>根据Token从业务系统Redis中获取用户信息</li>
 *     <li>校验用户的文件预览权限（fileAuth字段）</li>
 * </ol>
 *
 * <p>权限说明：
 * <ul>
 *     <li>fileAuth = "" (空字符串)：无任何权限</li>
 *     <li>fileAuth = "preview"：有预览权限</li>
 *     <li>fileAuth = "download"：有预览和下载权限</li>
 * </ul>
 *
 * <p>Redis用户信息存储格式：
 * <ul>
 *     <li>Key：USER-{token值}</li>
 *     <li>Value：JSON字符串，包含fileAuth字段</li>
 * </ul>
 *
 * @author kkFileView
 * @since 2026/04/15
 */
public class AuthFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    /**
     * Cookie中Token的名称
     */
    private static final String TOKEN_COOKIE_NAME = "TOKEN";

    /**
     * Redis中用户信息Key的前缀
     */
    private static final String USER_KEY_PREFIX = "USER-";

    /**
     * 用户信息JSON中权限字段的名称
     */
    private static final String FILE_AUTH_FIELD = "fileAuth";

    /**
     * 权限值：仅预览权限
     */
    private static final String AUTH_PREVIEW = "preview";

    /**
     * 权限值：预览和下载权限
     */
    private static final String AUTH_DOWNLOAD = "download";

    /**
     * 业务系统Redis客户端
     * 独立于系统自身的缓存Redis
     */
    private final RedissonClient authRedissonClient;

    /**
     * JSON解析工具
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 未登录提示页面HTML内容
     */
    private String notLoginHtmlView;

    /**
     * 登录失效提示页面HTML内容
     */
    private String loginExpiredHtmlView;

    /**
     * 无权限提示页面HTML内容
     */
    private String noPermissionHtmlView;

    /**
     * 构造函数
     *
     * @param authRedissonClient 业务系统Redis客户端
     */
    public AuthFilter(RedissonClient authRedissonClient) {
        this.authRedissonClient = authRedissonClient;
    }

    /**
     * 过滤器初始化方法
     * 加载错误提示页面的HTML内容
     *
     * @param filterConfig 过滤器配置
     */
    @Override
    public void init(FilterConfig filterConfig) {
        loadErrorPage("web/notLogin.html", html -> this.notLoginHtmlView = html);
        loadErrorPage("web/loginExpired.html", html -> this.loginExpiredHtmlView = html);
        loadErrorPage("web/noPermission.html", html -> this.noPermissionHtmlView = html);
    }

    /**
     * 加载错误提示页面
     *
     * @param path   页面资源路径
     * @param setter 页面内容设置函数
     */
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

    /**
     * 过滤器核心方法
     * 执行用户认证和权限校验
     *
     * <p>认证流程：
     * <ol>
     *     <li>如果Redis客户端未配置，直接放行（兼容未启用认证的场景）</li>
     *     <li>从Cookie中获取Token，无Token则提示"请先登录系统"</li>
     *     <li>根据Token从Redis获取用户信息，不存在则提示"登录状态已失效"</li>
     *     <li>校验用户权限，无权限则提示"无相关权限"</li>
     *     <li>校验通过，放行请求</li>
     * </ol>
     *
     * @param request  请求对象
     * @param response 响应对象
     * @param chain    过滤器链
     * @throws IOException      IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (authRedissonClient == null) {
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
        RBucket<String> userBucket = authRedissonClient.getBucket(userKey);
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

    /**
     * 从Cookie中获取用户Token
     *
     * @param request HTTP请求对象
     * @return Token值，如果不存在则返回null
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
     * 校验用户是否有文件预览权限
     *
     * <p>权限判断逻辑：
     * <ul>
     *     <li>fileAuth = "preview"：有预览权限</li>
     *     <li>fileAuth = "download"：有预览权限（同时有下载权限）</li>
     *     <li>其他值（包括空字符串）：无权限</li>
     * </ul>
     *
     * @param userInfoJson 用户信息JSON字符串
     * @return true-有权限，false-无权限
     */
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

    /**
     * 写入错误响应页面
     *
     * @param response HTTP响应对象
     * @param html     错误页面HTML内容
     * @throws IOException IO异常
     */
    private void writeResponse(HttpServletResponse response, String html) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(html);
        response.getWriter().close();
    }

    /**
     * 过滤器销毁方法
     */
    @Override
    public void destroy() {
    }
}
