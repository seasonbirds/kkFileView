package cn.keking.web.filter;

import cn.keking.service.UserService;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 登录过滤器
 * 拦截所有非静态资源请求，验证用户登录状态
 * 未登录用户自动重定向到登录页面
 */
public class LoginFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LoginFilter.class);

    /**
     * 登录Token在Cookie中的名称
     */
    public static final String LOGIN_COOKIE_NAME = "KK_USER_TOKEN";

    /**
     * 静态资源文件扩展名集合
     * 这些文件无需登录即可访问
     */
    private static final Set<String> STATIC_RESOURCE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".ico", ".svg",
            ".woff", ".woff2", ".ttf", ".eot",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".mp3", ".mp4", ".wav", ".flv",
            ".html", ".htm"
    ));

    /**
     * 排除路径集合
     * 这些路径无需登录即可访问
     */
    private static final Set<String> EXCLUDE_PATHS = new HashSet<>(Arrays.asList(
            "/login", "/login.html", "/doLogin", "/logout",
            "/actuator", "/actuator/health", "/actuator/info", "/actuator/metrics"
    ));

    /**
     * 用户服务
     */
    private UserService userService;

    /**
     * 过滤器初始化方法
     * 从Spring容器中获取UserService实例
     * 
     * @param filterConfig 过滤器配置
     */
    @Override
    public void init(FilterConfig filterConfig) {
        ServletContext servletContext = filterConfig.getServletContext();
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        if (ctx != null) {
            userService = ctx.getBean(UserService.class);
        }
    }

    /**
     * 过滤器核心方法
     * 验证用户登录状态，未登录用户重定向到登录页面
     * 
     * @param request 请求对象
     * @param response 响应对象
     * @param chain 过滤器链
     * @throws IOException IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String servletPath = httpRequest.getServletPath();

        // 排除路径直接放行
        if (isExcludedPath(servletPath)) {
            chain.doFilter(request, response);
            return;
        }

        // 静态资源直接放行
        if (isStaticResource(servletPath)) {
            chain.doFilter(request, response);
            return;
        }

        // 从请求中获取Token
        String token = getTokenFromRequest(httpRequest);

        // 验证Token
        if (StringUtils.isBlank(token) || !userService.validateToken(token)) {
            logger.info("用户未登录或Token无效，重定向到登录页面: path={}", servletPath);
            redirectToLogin(httpRequest, httpResponse);
            return;
        }

        // Token有效，继续执行过滤器链
        chain.doFilter(request, response);
    }

    /**
     * 判断是否为排除路径
     * 
     * @param servletPath 请求路径
     * @return 是排除路径返回true，否则返回false
     */
    private boolean isExcludedPath(String servletPath) {
        for (String excludePath : EXCLUDE_PATHS) {
            if (servletPath.startsWith(excludePath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否为静态资源
     * 
     * @param servletPath 请求路径
     * @return 是静态资源返回true，否则返回false
     */
    private boolean isStaticResource(String servletPath) {
        String lowerPath = servletPath.toLowerCase();
        
        // 根据文件扩展名判断
        for (String extension : STATIC_RESOURCE_EXTENSIONS) {
            if (lowerPath.endsWith(extension)) {
                return true;
            }
        }
        
        // 根据路径前缀判断
        if (servletPath.startsWith("/css/") || 
            servletPath.startsWith("/js/") || 
            servletPath.startsWith("/img/") ||
            servletPath.startsWith("/images/") ||
            servletPath.startsWith("/bootstrap/") ||
            servletPath.startsWith("/bootstrap-table/") ||
            servletPath.startsWith("/ckplayer/") ||
            servletPath.startsWith("/bpmn/") ||
            servletPath.startsWith("/dcm/")) {
            return true;
        }
        
        return false;
    }

    /**
     * 从请求中获取Token
     * 支持三种方式：
     * 1. Header中的Authorization: Bearer xxx
     * 2. Cookie中的KK_USER_TOKEN
     * 3. URL参数中的token
     * 
     * @param request HTTP请求对象
     * @return Token字符串，未找到返回null
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        
        // 从Header中获取（Bearer token）
        if (StringUtils.isNotBlank(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 从Cookie中获取
        if (StringUtils.isBlank(token)) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (LOGIN_COOKIE_NAME.equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }

        // 从URL参数中获取
        if (StringUtils.isBlank(token)) {
            token = request.getParameter("token");
        }

        return token;
    }

    /**
     * 重定向到登录页面
     * 对于Ajax请求返回401状态码，普通请求返回302重定向
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws IOException IO异常
     */
    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contextPath = request.getContextPath();
        String baseUrl = BaseUrlFilter.getBaseUrl();
        
        // 构建登录页面URL
        String loginUrl;
        if (StringUtils.isNotBlank(baseUrl)) {
            loginUrl = baseUrl + "login";
        } else {
            loginUrl = contextPath + "/login";
        }

        // Ajax请求返回401状态码和JSON响应
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\": 401, \"msg\": \"未登录或登录已过期\", \"content\": null}");
            return;
        }

        // 普通请求重定向到登录页面
        response.sendRedirect(loginUrl);
    }

    /**
     * 过滤器销毁方法
     */
    @Override
    public void destroy() {
    }
}
