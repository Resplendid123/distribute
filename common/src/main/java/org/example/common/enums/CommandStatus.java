package org.example.common.enums;

/**
 * 命令状态枚举
 * 定义了命令在整个生命周期中的所有可能状态
 */
public enum CommandStatus {
    /**
     * 待执行 - 命令已创建，等待转发到Agent
     */
    PENDING("pending", "待执行"),

    /**
     * 执行中 - 命令已转发给Agent，正在执行
     */
    EXECUTING("executing", "执行中"),

    /**
     * 已完成 - 命令执行成功
     */
    COMPLETED("completed", "已完成"),

    /**
     * 失败 - 命令执行失败
     */
    FAILED("failed", "失败"),

    /**
     * 超时 - 命令执行超时
     */
    TIMEOUT("timeout", "超时"),

    /**
     * 已取消 - 命令被取消
     */
    CANCELLED("cancelled", "已取消");

    private final String code;
    private final String description;

    CommandStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据 code获取对应的枚举值
     *
     * @param code 状态码
     * @return 对应的CommandStatus枚举值，如果不存在则返回null
     */
    public static CommandStatus fromCode(String code) {
        for (CommandStatus status : CommandStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断状态是否为最终状态（不会再改变）
     *
     * @return true表示为最终状态，false表示可能还会改变
     */
    public boolean isFinal() {
        return this == COMPLETED || this == FAILED || this == TIMEOUT || this == CANCELLED;
    }

    /**
     * 判断状态是否为运行状态（命令正在被处理）
     *
     * @return true 表示为运行状态
     */
    public boolean isRunning() {
        return this == PENDING || this == EXECUTING;
    }
}
