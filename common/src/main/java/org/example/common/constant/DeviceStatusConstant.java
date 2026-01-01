package org.example.common.constant;

/**
 * 设备状态常量
 * 提供设备状态的数值常量
 */
public class DeviceStatusConstant {
    private DeviceStatusConstant() {
        throw new AssertionError("Cannot instantiate constant class");
    }

    // 离线状态 - 设备未连接或连接已断开
    public static final Integer OFFLINE = 0;

    // 在线状态 - 设备已连接并正常工作
    public static final Integer ONLINE = 1;

    // 故障状态 - 设备出现异常或故障
    public static final Integer FAULT = 2;
}
