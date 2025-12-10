package cn.keking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * IP白名单配置
 * 
 * @author kl
 */
@Component
@ConfigurationProperties(prefix = "ip.whitelist")
public class IpWhitelistConfig {

    /**
     * 是否启用IP白名单
     */
    private boolean enabled = false;

    /**
     * 白名单IP地址列表，支持单个IP和IP段
     * 例如：192.168.1.100, 192.168.1.1-192.168.1.255
     */
    private List<String> allowedIps = Arrays.asList("127.0.0.1", "localhost");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(List<String> allowedIps) {
        this.allowedIps = allowedIps;
    }
}