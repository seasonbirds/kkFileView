package cn.keking.config;

import cn.keking.web.filter.AuthFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

@ConditionalOnExpression("'${cache.type:default}'.equals('redis') and ${auth.enabled:false}")
@Configuration
public class AuthFilterConfig {

    @Bean
    public FilterRegistrationBean<AuthFilter> getAuthFilter(RedissonClient redissonClient) {
        Set<String> filterUri = new HashSet<>();
        filterUri.add("/onlinePreview");
        filterUri.add("/picturesPreview");
        filterUri.add("/getCorsFile");
        AuthFilter filter = new AuthFilter(redissonClient);
        FilterRegistrationBean<AuthFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(filter);
        registrationBean.setUrlPatterns(filterUri);
        registrationBean.setOrder(5);
        return registrationBean;
    }
}
