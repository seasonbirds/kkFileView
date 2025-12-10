package cn.keking.utils;

import cn.keking.config.IpWhitelistConfig;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * IP白名单工具类
 * 
 * @author kl
 */
@Component
public class IpWhitelistUtils {

    private final IpWhitelistConfig ipWhitelistConfig;

    public IpWhitelistUtils(IpWhitelistConfig ipWhitelistConfig) {
        this.ipWhitelistConfig = ipWhitelistConfig;
    }

    /**
     * 检查IP地址是否在白名单中
     * 
     * @param request HTTP请求
     * @return 是否在白名单中
     */
    public boolean isIpAllowed(HttpServletRequest request) {
        // 如果白名单未启用，直接返回true
        if (!ipWhitelistConfig.isEnabled()) {
            return true;
        }

        String ipAddress = getClientIpAddress(request);
        return isIpInWhitelist(ipAddress, ipWhitelistConfig.getAllowedIps());
    }

    /**
     * 获取客户端IP地址
     * 
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // 如果存在多个代理，取第一个IP
            int index = xForwardedFor.indexOf(',');
            if (index != -1) {
                return xForwardedFor.substring(0, index).trim();
            } else {
                return xForwardedFor.trim();
            }
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }

    /**
     * 检查IP是否在白名单中
     * 
     * @param ipAddress 要检查的IP地址
     * @param allowedIps 白名单列表
     * @return 是否在白名单中
     */
    private boolean isIpInWhitelist(String ipAddress, List<String> allowedIps) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }

        for (String allowedIp : allowedIps) {
            if (isIpMatch(ipAddress, allowedIp.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查单个IP是否匹配白名单规则
     * 
     * @param ipAddress 要检查的IP地址
     * @param allowedIp 白名单规则
     * @return 是否匹配
     */
    private boolean isIpMatch(String ipAddress, String allowedIp) {
        // 处理localhost
        if ("localhost".equals(allowedIp)) {
            return "127.0.0.1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress);
        }

        // 检查是否是IP段（格式：192.168.1.1-192.168.1.255）
        if (allowedIp.contains("-")) {
            return isIpInRange(ipAddress, allowedIp);
        }

        // 精确匹配
        return ipAddress.equals(allowedIp);
    }

    /**
     * 检查IP是否在指定范围内
     * 
     * @param ipAddress 要检查的IP地址
     * @param ipRange IP范围（格式：192.168.1.1-192.168.1.255）
     * @return 是否在范围内
     */
    private boolean isIpInRange(String ipAddress, String ipRange) {
        try {
            String[] parts = ipRange.split("-");
            if (parts.length != 2) {
                return false;
            }

            String startIp = parts[0].trim();
            String endIp = parts[1].trim();

            // 只支持第四段的范围匹配
            String[] ipParts = ipAddress.split("\\.");
            String[] startParts = startIp.split("\\.");
            String[] endParts = endIp.split("\\.");

            if (ipParts.length != 4 || startParts.length != 4 || endParts.length != 4) {
                return false;
            }

            // 检查前三段是否相同
            for (int i = 0; i < 3; i++) {
                if (!ipParts[i].equals(startParts[i]) || !ipParts[i].equals(endParts[i])) {
                    return false;
                }
            }

            // 检查第四段是否在范围内
            int ipLastPart = Integer.parseInt(ipParts[3]);
            int startLastPart = Integer.parseInt(startParts[3]);
            int endLastPart = Integer.parseInt(endParts[3]);

            return ipLastPart >= startLastPart && ipLastPart <= endLastPart;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}