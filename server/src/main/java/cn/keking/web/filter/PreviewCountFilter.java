package cn.keking.web.filter;

import cn.keking.service.FileRankService;
import cn.keking.utils.WebUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.io.IOException;

/**
 * 预览次数统计过滤器
 * 非侵入式统计/onlinePreview接口的调用次数
 * 只有在预览请求成功后才计数
 */
public class PreviewCountFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewCountFilter.class);
    private FileRankService fileRankService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 懒加载FileRankService，避免依赖注入问题
        if (fileRankService == null) {
            ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(request.getServletContext());
            if (context != null) {
                fileRankService = context.getBean(FileRankService.class);
            }
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        StatusCaptureResponseWrapper responseWrapper = new StatusCaptureResponseWrapper((HttpServletResponse) response);

        String fileName = null;
        try {
            // 提前解析文件名（不计数）
            String urlParam = httpRequest.getParameter("url");
            if (urlParam != null && fileRankService != null) {
                String fileUrl = WebUtils.decodeUrl(urlParam);
                if (fileUrl != null) {
                    fileName = getFileName(fileUrl, httpRequest);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("解析文件名异常", e);
        }

        try {
            // 执行原请求
            chain.doFilter(request, responseWrapper);
        } finally {
            // 请求完成后检查状态码，如果成功则计数
            if (fileName != null && !fileName.isEmpty() && fileRankService != null) {
                int status = responseWrapper.getStatus();
                if (status >= 200 && status < 400) {
                    try {
                        fileRankService.incrementPreviewCount(fileName);
                        LOGGER.debug("统计预览次数: {}, 状态码: {}", fileName, status);
                    } catch (Exception e) {
                        // 统计异常不影响主业务
                        LOGGER.warn("统计预览次数异常", e);
                    }
                } else {
                    LOGGER.debug("预览请求失败，不统计次数: {}, 状态码: {}", fileName, status);
                }
            }
        }
    }

    private String getFileName(String fileUrl, HttpServletRequest request) {
        // 优先从fullfilename参数获取文件名（与OnlinePreviewController逻辑一致）
        String fullFileName = WebUtils.getUrlParameterReg(fileUrl, "fullfilename");
        if (fullFileName != null && !fullFileName.isEmpty()) {
            return fullFileName;
        }
        // 从URL中提取文件名
        return WebUtils.getFileNameFromURL(fileUrl);
    }

    /**
     * 用于捕获响应状态的包装器
     */
    private static class StatusCaptureResponseWrapper extends HttpServletResponseWrapper {
        private int status = SC_OK;

        public StatusCaptureResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            super.setStatus(sc);
            this.status = sc;
        }

        @Override
        public void sendError(int sc) throws IOException {
            super.sendError(sc);
            this.status = sc;
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            super.sendError(sc, msg);
            this.status = sc;
        }

        @Override
        public int getStatus() {
            return this.status;
        }
    }
}
