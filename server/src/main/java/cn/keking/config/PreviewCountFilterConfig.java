package cn.keking.config;

import cn.keking.filter.PreviewCountFilter;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件预览统计功能过滤器配置
 * @author kkfileview
 */
@Configuration
public class PreviewCountFilterConfig {
    
    @Bean
    public PreviewCountFilter previewCountFilter() {
        return new PreviewCountFilter();
    }
    
    @Bean
    public DelegatingFilterProxyRegistrationBean previewCountFilterRegistration() {
        DelegatingFilterProxyRegistrationBean registration = new DelegatingFilterProxyRegistrationBean("previewCountFilter");
        registration.addUrlPatterns("/onlinePreview");
        registration.setName("previewCountFilter");
        registration.setOrder(1); // 设置过滤器顺序
        return registration;
    }
}