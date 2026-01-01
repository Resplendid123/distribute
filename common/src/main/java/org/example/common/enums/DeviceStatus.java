package org.example.common.enums;

/**
 * 设备状态枚举
 * 定义了设备的在线/离线状态
 */
public enum DeviceStatus {
    /**
     * 离线状态 - 设备未连接或连接已断开
     */
    OFFLINE(0, "离线"),

    /**
     * 在线状态 - 设备已连接并正常工作
     */
    ONLINE(1, "在线"),

    /**
     * 故障状态 - 设备出现异常或故障
     */
    FAULT(2, "故障");

    private final Integer code;
    private final String description;

    DeviceStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取对应的枚举值
     *
     * @param code 状态码
     * @return 对应的DeviceStatus枚举值，如果不存在则返回null
     */
    public static DeviceStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DeviceStatus status : DeviceStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断设备是否在线
     *
     * @return true表示设备在线
     */
    public boolean isOnline() {
        return this == ONLINE;
    }
}
