package cn.keking.web.filter.ratelimit;

import cn.keking.config.ConfigConstants;
import cn.keking.utils.WebUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 限流Filter
 * 对/onlinePreview接口进行限流
 * 基于IP地址的限流，使用Filter实现，对接口代码无侵入
 */
public class RateLimitFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String RATE_LIMIT_MESSAGE = "请求太频繁，请稍后再试";

    private IpRateLimiter rateLimiter;

    @Override
    public void init(FilterConfig filterConfig) {
        try {
            int windowSeconds = ConfigConstants.getRateLimitWindowSeconds();
            int maxRequests = ConfigConstants.getRateLimitMaxRequests();

            if (windowSeconds <= 0) {
                logger.warn("Invalid rate limit window seconds: {}, using default: 60", windowSeconds);
                windowSeconds = 60;
            }
            if (maxRequests <= 0) {
                logger.warn("Invalid rate limit max requests: {}, using default: 10", maxRequests);
                maxRequests = 10;
            }

            rateLimiter = new IpRateLimiter(windowSeconds, maxRequests);
            logger.info("RateLimitFilter initialized: window={}s, maxRequests={}", windowSeconds, maxRequests);

        } catch (Exception e) {
            logger.error("Failed to initialize RateLimitFilter", e);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (rateLimiter == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String clientIp = WebUtils.getClientIp(httpRequest);

            if (rateLimiter.isAllowed(clientIp)) {
                chain.doFilter(request, response);
            } else {
                handleRateLimitExceeded(response, clientIp);
            }

        } catch (Exception e) {
            logger.error("Error in RateLimitFilter, allowing request to proceed", e);
            chain.doFilter(request, response);
        }
    }

    private void handleRateLimitExceeded(ServletResponse response, String clientIp) throws IOException {
        logger.warn("Rate limit exceeded for IP: {}", clientIp);

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setStatus(429);
        httpResponse.setContentType("text/html;charset=UTF-8");
        httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String htmlResponse = buildHtmlResponse();
        httpResponse.getWriter().write(htmlResponse);
        httpResponse.getWriter().flush();
    }

    private String buildHtmlResponse() {
        return "<!DOCTYPE html>" +
                "<html lang=\"zh-CN\">" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>请求频繁</title>" +
                "<style>" +
                "body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; " +
                "       display: flex; justify-content: center; align-items: center; " +
                "       min-height: 100vh; margin: 0; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }" +
                ".container { text-align: center; padding: 40px; background: white; " +
                "             border-radius: 16px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }" +
                ".icon { font-size: 64px; margin-bottom: 20px; }" +
                "h1 { color: #333; margin-bottom: 16px; }" +
                "p { color: #666; font-size: 18px; margin-bottom: 24px; }" +
                ".btn { display: inline-block; padding: 12px 32px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); " +
                "       color: white; text-decoration: none; border-radius: 25px; " +
                "       font-weight: 500; transition: transform 0.2s, box-shadow 0.2s; }" +
                ".btn:hover { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4); }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class=\"container\">" +
                "<div class=\"icon\">⏳</div>" +
                "<h1>请求太频繁</h1>" +
                "<p>" + RATE_LIMIT_MESSAGE + "</p>" +
                "<a href=\"javascript:history.back()\" class=\"btn\">返回上一页</a>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    @Override
    public void destroy() {
        logger.info("RateLimitFilter destroyed");
    }
}
