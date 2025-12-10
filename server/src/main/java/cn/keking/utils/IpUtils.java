package cn.keking.utils;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * IP工具类，用于IP地址校验和白名单检查
 *
 * @author kl
 */
public class IpUtils {

    /**
     * IP地址正则表达式
     */
    private static final Pattern IP_PATTERN = Pattern.compile("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    /**
     * 检查IP是否在白名单中
     * 支持精确IP地址和IP地址段（仅第四段支持范围，如：192.168.1.1-100）
     *
     * @param ip        待检查的IP地址
     * @param whiteList IP白名单集合
     * @return 是否在白名单中
     */
    public static boolean isIpInWhiteList(String ip, Set<String> whiteList) {
        if (ip == null || whiteList == null || whiteList.isEmpty()) {
            return false;
        }

        // 检查精确匹配
        if (whiteList.contains(ip)) {
            return true;
        }

        // 检查IP地址段匹配
        for (String whiteIp : whiteList) {
            if (whiteIp.contains("-")) {
                if (isIpInRange(ip, whiteIp)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查IP是否在指定范围内
     * 格式：192.168.1.1-100
     *
     * @param ip   待检查的IP
     * @param range IP范围
     * @return 是否在范围内
     */
    private static boolean isIpInRange(String ip, String range) {
        try {
            // 解析IP范围
            int hyphenIndex = range.lastIndexOf('-');
            if (hyphenIndex == -1) {
                return false;
            }

            String baseIp = range.substring(0, hyphenIndex);
            // 检查baseIp是否是有效的三段IP
            if (!baseIp.matches("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}$")) {
                return false;
            }

            // 解析范围
            String rangeStr = range.substring(hyphenIndex + 1);
            int start, end;
            if (rangeStr.contains("-")) {
                // 格式：192.168.1.10-100
                String[] rangeParts = rangeStr.split("-");
                if (rangeParts.length != 2) {
                    return false;
                }
                start = Integer.parseInt(rangeParts[0]);
                end = Integer.parseInt(rangeParts[1]);
            } else {
                // 格式：192.168.1.1-100（只有一个范围值）
                start = Integer.parseInt(rangeStr);
                end = 255;
            }

            // 检查IP格式
            if (!IP_PATTERN.matcher(ip).matches()) {
                return false;
            }

            // 提取IP的第四段
            String[] ipParts = ip.split("\\.");
            int ipFourthSegment = Integer.parseInt(ipParts[3]);

            // 检查IP的前三段是否匹配
            String ipFirstThreeSegments = ip.substring(0, ip.lastIndexOf('.')) + ".";
            if (!ipFirstThreeSegments.equals(baseIp)) {
                return false;
            }

            // 检查第四段是否在范围内
            return ipFourthSegment >= start && ipFourthSegment <= end;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 从请求中获取真实IP地址
     *
     * @param request HTTP请求
     * @return IP地址
     */
    public static String getRealIpFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个代理的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}