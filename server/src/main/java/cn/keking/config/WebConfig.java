package cn.keking.config;

import cn.keking.service.PreviewRankService;
import cn.keking.web.filter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.HashSet;
import java.util.Set;

/**
 * Web配置类
 * 配置静态资源处理和过滤器注册
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final static Logger LOGGER = LoggerFactory.getLogger(WebConfig.class);

    /**
     * 配置静态资源处理
     * 添加文件存储目录为静态资源路径
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String filePath = ConfigConstants.getFileDir();
        LOGGER.info("Add resource locations: {}", filePath);
        registry.addResourceHandler("/**").addResourceLocations("classpath:/META-INF/resources/","classpath:/resources/","classpath:/static/","classpath:/public/","file:" + filePath);
    }

    /**
     * 注册中文路径过滤器
     * 处理URL中的中文路径编码问题
     */
    @Bean
    public FilterRegistrationBean<ChinesePathFilter> getChinesePathFilter() {
        ChinesePathFilter filter = new ChinesePathFilter();
        FilterRegistrationBean<ChinesePathFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(10);
        return registrationBean;
    }

    /**
     * 注册信任主机过滤器
     * 校验请求来源是否在信任主机白名单中
     */
    @Bean
    public FilterRegistrationBean<TrustHostFilter> getTrustHostFilter() {
        Set<String> filterUri = new HashSet<>();
        filterUri.add("/onlinePreview");
        filterUri.add("/picturesPreview");
        filterUri.add("/getCorsFile");
        TrustHostFilter filter = new TrustHostFilter();
        FilterRegistrationBean<TrustHostFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setUrlPatterns(filterUri);
        return registrationBean;
    }

    /**
     * 注册信任目录过滤器
     * 校验请求路径是否在允许访问的目录范围内
     */
    @Bean
    public FilterRegistrationBean<TrustDirFilter> getTrustDirFilter() {
        Set<String> filterUri = new HashSet<>();
        filterUri.add("/onlinePreview");
        filterUri.add("/picturesPreview");
        filterUri.add("/getCorsFile");
        TrustDirFilter filter = new TrustDirFilter();
        FilterRegistrationBean<TrustDirFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setUrlPatterns(filterUri);
        return registrationBean;
    }

    /**
     * 注册基础URL过滤器
     * 设置请求的基础URL信息
     */
    @Bean
    public FilterRegistrationBean<BaseUrlFilter> getBaseUrlFilter() {
        Set<String> filterUri = new HashSet<>();
        BaseUrlFilter filter = new BaseUrlFilter();
        FilterRegistrationBean<BaseUrlFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setUrlPatterns(filterUri);
        registrationBean.setOrder(20);
        return registrationBean;
    }

    /**
     * 注册URL校验过滤器
     * 校验请求URL的合法性
     */
    @Bean
    public FilterRegistrationBean<UrlCheckFilter> getUrlCheckFilter() {
        UrlCheckFilter filter = new UrlCheckFilter();
        FilterRegistrationBean<UrlCheckFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(30);
        return registrationBean;
    }

    /**
     * 注册属性设置过滤器
     * 为请求设置水印等属性信息
     */
    @Bean
    public FilterRegistrationBean<AttributeSetFilter> getWatermarkConfigFilter() {
        Set<String> filterUri = new HashSet<>();
        filterUri.add("/index");
        filterUri.add("/");
        filterUri.add("/onlinePreview");
        filterUri.add("/picturesPreview");
        AttributeSetFilter filter = new AttributeSetFilter();
        FilterRegistrationBean<AttributeSetFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setUrlPatterns(filterUri);
        return registrationBean;
    }

    /**
     * 注册预览排行榜统计过滤器
     * 拦截/onlinePreview请求，在预览完成后异步记录统计数据
     * 执行顺序设为100，确保在其他过滤器之后执行
     */
    @Bean
    public FilterRegistrationBean<PreviewRankFilter> getPreviewRankFilter(PreviewRankService previewRankService) {
        Set<String> filterUri = new HashSet<>();
        filterUri.add("/onlinePreview");
        PreviewRankFilter filter = new PreviewRankFilter(previewRankService);
        FilterRegistrationBean<PreviewRankFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setUrlPatterns(filterUri);
        registrationBean.setOrder(100);
        return registrationBean;
    }
}
