package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import cn.keking.service.AlertEmailService;
import cn.keking.service.AsyncDatabaseService;
import cn.keking.service.UserBehaviorAnalysisService;
import cn.keking.service.UserBehaviorAnalysisService.RateLimitResult;
import cn.keking.service.UserBehaviorCache;
import cn.keking.service.database.SQLiteConnectionManager;
import cn.keking.service.database.UserBehaviorDao;
import cn.keking.utils.WebUtils;

import jakarta.servlet.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

/**
 * 用户行为监控Filter
 * 
 * <p>实现对预览请求的无侵入式拦截，主要功能包括：</p>
 * <ol>
 *   <li>在Filter初始化时创建分析服务和数据库连接</li>
 *   <li>拦截每个预览请求，记录用户行为并执行限流检查</li>
 *   <li>限流时返回友好的提示页面，不影响正常用户访问</li>
 *   <li>在Filter销毁时优雅关闭线程池资源</li>
 * </ol>
 * 
 * <p><strong>容错设计：</strong>所有监控处理逻辑都包含在try-catch中，发生异常时放行请求，确保用户正常使用</p>
 * 
 * @author kkFileView Team
 */
public class UserBehaviorFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserBehaviorFilter.class);

    private UserBehaviorAnalysisService analysisService;
    private String rateLimitHtmlView;

    /**
     * Filter初始化方法
     * 
     * <p>初始化流程：</p>
     * <ol>
     *   <li>检查用户行为监控开关是否开启</li>
     *   <li>初始化SQLite数据库</li>
     *   <li>依次创建Dao、邮件服务、本地缓存、异步数据库服务</li>
     *   <li>组装分析服务</li>
     *   <li>加载限流提示页面模板</li>
     * </ol>
     * 
     * <p>所有初始化异常都被捕获并记录日志，功能自动禁用，不影响系统启动</p>
     *
     * @param filterConfig Filter配置对象
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        if (!ConfigConstants.isUserBehaviorMonitorEnabled()) {
            LOGGER.info("用户行为监控已禁用，跳过初始化");
            return;
        }

        try {
            SQLiteConnectionManager.initializeDatabase();

            UserBehaviorDao userBehaviorDao = new UserBehaviorDao();
            AlertEmailService alertEmailService = new AlertEmailService();
            UserBehaviorCache userBehaviorCache = new UserBehaviorCache();
            AsyncDatabaseService asyncDatabaseService = new AsyncDatabaseService(userBehaviorDao);

            this.analysisService = new UserBehaviorAnalysisService(
                    alertEmailService, userBehaviorCache, asyncDatabaseService);

            ClassPathResource classPathResource = new ClassPathResource("web/rateLimit.html");
            byte[] bytes = FileCopyUtils.copyToByteArray(classPathResource.getInputStream());
            this.rateLimitHtmlView = new String(bytes, StandardCharsets.UTF_8);

            LOGGER.info("用户行为监控 Filter 初始化完成");
        } catch (Exception e) {
            LOGGER.error("用户行为监控 Filter 初始化失败，功能将被禁用", e);
        }
    }

    /**
     * 请求拦截主方法 - 带容错保护
     * 
     * <p>执行流程：</p>
     * <ol>
     *   <li>检查功能开关，未开启则直接放行</li>
     *   <li>将实际业务逻辑委托给doFilterInternal()方法</li>
     *   <li>捕获所有异常并放行请求，确保用户正常访问不受影响</li>
     * </ol>
     * 
     * <p><strong>核心容错设计：</strong>任何监控处理异常都不会阻断用户请求</p>
     *
     * @param request  Servlet请求对象
     * @param response Servlet响应对象
     * @param chain    Filter链
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!ConfigConstants.isUserBehaviorMonitorEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            doFilterInternal(request, response, chain);
        } catch (Exception e) {
            LOGGER.error("用户行为监控处理异常，放行请求", e);
            chain.doFilter(request, response);
        }
    }

    /**
     * 请求拦截核心逻辑
     * 
     * <p>执行流程：</p>
     * <ol>
     *   <li>检查分析服务是否初始化成功，失败则直接放行</li>
     *   <li>使用WebUtils工具类获取客户端IP地址（支持多种代理场景）</li>
     *   <li>提取文件名和完整请求URL</li>
     *   <li>调用分析服务异步记录行为（使用线程池，不阻塞主流程）</li>
     *   <li>使用本地缓存执行限流检查（高效、低延迟）</li>
     *   <li>被限流则返回友好提示页面</li>
     *   <li>通过检查则放行请求到下游Filter</li>
     * </ol>
     * 
     * <p><strong>性能优化：</strong>限流检查使用内存缓存，避免每次请求都查询数据库</p>
     *
     * @param request  Servlet请求对象
     * @param response Servlet响应对象
     * @param chain    Filter链
     */
    private void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (analysisService == null) {
            chain.doFilter(request, response);
            return;
        }

        String ipAddress = WebUtils.getClientIpAddress(request);
        String sourceUrl = WebUtils.getSourceUrl(request);
        String fileName = (sourceUrl != null) ? WebUtils.getFileNameFromURL(sourceUrl) : "unknown";
        String requestUrl = WebUtils.getFullRequestUrl(request);

        LOGGER.debug("记录请求: IP={}, 文件名={}, URL={}", ipAddress, fileName, requestUrl);

        analysisService.recordBehavior(ipAddress, fileName, requestUrl);

        RateLimitResult result = analysisService.checkRateLimit(ipAddress);
        if (!result.isAllowed()) {
            LOGGER.warn("IP [{}] 被限流: {}", ipAddress, result.getMessage());
            String html = rateLimitHtmlView.replace("${message}", result.getMessage());
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write(html);
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * Filter销毁方法 - 优雅关闭资源
     * 
     * <p>调用分析服务的shutdown()方法，依次关闭：</p>
     * <ul>
     *   <li>每日重置任务调度器</li>
     *   <li>本地缓存清理线程</li>
     *   <li>异步数据库写入线程池</li>
     * </ul>
     */
    @Override
    public void destroy() {
        if (analysisService != null) {
            analysisService.shutdown();
        }
    }
}
