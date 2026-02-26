package cn.keking.service;

/**
 * 告警邮件服务接口
 * @author kkfileview
 */
public interface AlertEmailService {

    /**
     * 发送用户行为异常告警邮件
     * @param ipAddress IP地址
     * @param windowMinutes 统计周期（分钟）
     * @param requestCount 请求次数
     */
    void sendAbnormalBehaviorAlert(String ipAddress, int windowMinutes, int requestCount);

    /**
     * 检查邮件配置是否有效
     * @return 是否有效
     */
    boolean isEmailConfigValid();
}
