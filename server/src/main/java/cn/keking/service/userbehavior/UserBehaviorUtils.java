package cn.keking.service.userbehavior;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户行为监控工具类
 * 提供通用的HTTP请求处理方法
 */
public class UserBehaviorUtils {

    /**
     * 需要监控的URL模式列表
     * 只有匹配这些模式的请求才会被监控
     */
    private static final String[] MONITOR_PATTERNS = {
        "/onlinePreview",
        "/picturesPreview"
    };

    /**
     * 判断请求URL是否需要监控
     *
     * @param requestUri 请求URI
     * @return true表示需要监控
     */
    public static boolean isMonitorUrl(String requestUri) {
        for (String pattern : MONITOR_PATTERNS) {
            if (requestUri.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取客户端真实IP地址
     * 优先从X-Forwarded-For和X-Real-IP头获取，支持代理场景
     *
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }

    /**
     * 从请求中提取文件名
     * 从url参数中解析文件名，支持URL编码
     *
     * @param request HTTP请求对象
     * @return 文件名，如果无法获取则返回"unknown"
     */
    public static String getFileName(HttpServletRequest request) {
        String url = request.getParameter("url");
        if (url != null && !url.isEmpty()) {
            try {
                String decodedUrl = java.net.URLDecoder.decode(url, "UTF-8");
                int lastSlash = decodedUrl.lastIndexOf('/');
                if (lastSlash > -1) {
                    return decodedUrl.substring(lastSlash + 1);
                }
                return decodedUrl;
            } catch (Exception e) {
                return url;
            }
        }
        return "unknown";
    }
}