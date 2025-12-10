package cn.keking.filter;

import cn.keking.config.ConfigConstants;
import cn.keking.utils.IpUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

/**
 * 源文件下载拦截器
 * 用于拦截源文件下载请求，进行功能开关检查和IP白名单验证
 */
@Component
public class SourceFileDownloadInterceptor implements HandlerInterceptor {

    /**
     * 请求前置处理，进行源文件下载权限验证
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @return true-继续处理请求，false-拒绝请求
     * @throws IOException IO异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 检查功能开关
        if (!ConfigConstants.isSourceFileDownloadEnabled()) {
            return errorResponse(response, "源文件下载功能未启用");
        }

        // 获取真实IP地址
        String clientIp = IpUtils.getRealIpFromRequest(request);

        // 获取IP白名单配置
        Set<String> ipWhiteList = ConfigConstants.getSourceFileDownloadIpWhiteList();

        // 检查IP白名单
        // 如果白名单为空或包含"*"，则允许所有IP访问（仅用于测试环境）
        if (ipWhiteList.isEmpty() || ipWhiteList.contains("*")) {
            return true;
        }

        // 验证IP是否在白名单中
        if (IpUtils.isIpInWhiteList(clientIp, ipWhiteList)) {
            return true;
        }

        // IP不在白名单中，拒绝请求
        return errorResponse(response, "IP地址不在允许的白名单中");
    }

    /**
     * 发送错误响应
     *
     * @param response HTTP响应
     * @param message  错误消息
     * @return false-拒绝请求
     * @throws IOException IO异常
     */
    private boolean errorResponse(jakarta.servlet.http.HttpServletResponse response, String message) throws IOException {
        response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.write("{\"success\":false, \"message\":\"" + message + "\"}");
        writer.flush();
        writer.close();
        return false;
    }

    /**
     * 请求处理后回调
     */
    @Override
    public void postHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // 不进行任何处理
    }

    /**
     * 请求完成后回调
     */
    @Override
    public void afterCompletion(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler, Exception ex) {
        // 不进行任何处理
    }
}