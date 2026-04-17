package cn.keking.web.filter;

import cn.keking.config.UserConfigConstants;
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

public class LoginFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(LoginFilter.class);

    public static final String LOGIN_COOKIE_NAME = "KK_USER_TOKEN";

    private static final Set<String> STATIC_RESOURCE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".ico", ".svg",
            ".woff", ".woff2", ".ttf", ".eot",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".mp3", ".mp4", ".wav", ".flv",
            ".html", ".htm"
    ));

    private static final Set<String> EXCLUDE_PATHS = new HashSet<>(Arrays.asList(
            "/login", "/login.html", "/doLogin", "/logout",
            "/actuator", "/actuator/health", "/actuator/info", "/actuator/metrics"
    ));

    private UserService userService;

    @Override
    public void init(FilterConfig filterConfig) {
        ServletContext servletContext = filterConfig.getServletContext();
        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        if (ctx != null) {
            userService = ctx.getBean(UserService.class);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!UserConfigConstants.isUserAuthEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        String servletPath = httpRequest.getServletPath();

        if (isExcludedPath(servletPath)) {
            chain.doFilter(request, response);
            return;
        }

        if (isStaticResource(servletPath)) {
            chain.doFilter(request, response);
            return;
        }

        String token = getTokenFromRequest(httpRequest);

        if (StringUtils.isBlank(token) || !userService.validateToken(token)) {
            logger.info("用户未登录或Token无效，重定向到登录页面: path={}", servletPath);
            redirectToLogin(httpRequest, httpResponse);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isExcludedPath(String servletPath) {
        for (String excludePath : EXCLUDE_PATHS) {
            if (servletPath.startsWith(excludePath)) {
                return true;
            }
        }
        return false;
    }

    private boolean isStaticResource(String servletPath) {
        String lowerPath = servletPath.toLowerCase();
        for (String extension : STATIC_RESOURCE_EXTENSIONS) {
            if (lowerPath.endsWith(extension)) {
                return true;
            }
        }
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

    private String getTokenFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

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

        if (StringUtils.isBlank(token)) {
            token = request.getParameter("token");
        }

        return token;
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contextPath = request.getContextPath();
        String baseUrl = BaseUrlFilter.getBaseUrl();
        
        String loginUrl;
        if (StringUtils.isNotBlank(baseUrl)) {
            loginUrl = baseUrl + "login";
        } else {
            loginUrl = contextPath + "/login";
        }

        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\": 401, \"msg\": \"未登录或登录已过期\", \"content\": null}");
            return;
        }

        response.sendRedirect(loginUrl);
    }

    @Override
    public void destroy() {
    }
}
