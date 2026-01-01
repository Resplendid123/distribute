package org.example.common.constant;

/**
 * 命令状态常量
 * 提供命令状态的字符串常量，用于数据库存储和兼容性
 */
public class CommandStatusConstant {
    private CommandStatusConstant() {
        throw new AssertionError("Cannot instantiate constant class");
    }

    // 待执行 - 命令已创建，等待转发到Agent
    public static final String PENDING = "pending";

    // 执行中 - 命令已转发给Agent，正在执行
    public static final String EXECUTING = "executing";

    // 已完成 - 命令执行成功
    public static final String COMPLETED = "completed";

    // 失败 - 命令执行失败
    public static final String FAILED = "failed";

    // 超时 - 命令执行超时
    public static final String TIMEOUT = "timeout";

    // 已取消 - 命令被取消
    public static final String CANCELLED = "cancelled";
}
