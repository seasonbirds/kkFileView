package cn.keking.web.filter;

import cn.keking.service.RankService;
import cn.keking.utils.WebUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

/**
 * 文件预览次数统计Filter
 * 用于拦截/onlinePreview接口，统计文件预览次数
 *
 * 实现说明：
 * 1. 使用Filter实现，对/onlinePreview接口无侵入性
 * 2. 在请求成功处理后才统计预览次数
 * 3. 所有异常都被捕获，确保不影响核心预览功能
 * 4. WebConfig中已配置只拦截/onlinePreview接口
 *
 * @author kkFileView
 */
public class PreviewCountFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(PreviewCountFilter.class);

    private final RankService rankService;

    public PreviewCountFilter(RankService rankService) {
        this.rankService = rankService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    /**
     * 执行过滤逻辑
     * 1. 解码URL参数获取文件URL和文件名
     * 2. 执行后续过滤器链（确保预览请求正常处理）
     * 3. 请求处理完成后，异步统计预览次数
     *
     * 注意：filterChain.doFilter只执行一次，避免重复调用
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param filterChain 过滤器链
     * @throws IOException IO异常
     * @throws ServletException Servlet异常
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String urlParam = httpRequest.getParameter("url");

        String fileUrl = null;
        String fileName = null;

        if (urlParam != null && !urlParam.isEmpty()) {
            try {
                fileUrl = WebUtils.decodeUrl(urlParam);
                if (fileUrl != null && !fileUrl.isEmpty()) {
                    fileName = WebUtils.getFileNameFromURL(fileUrl);
                }
            } catch (Exception e) {
                logger.warn("Failed to decode url for preview count: {}", urlParam, e);
            }
        }

        filterChain.doFilter(request, response);

        if (fileUrl != null && !fileUrl.isEmpty()) {
            try {
                rankService.incrementPreviewCount(fileUrl, fileName);
            } catch (Exception e) {
                logger.warn("Failed to increment preview count for file: {}", fileUrl, e);
            }
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
