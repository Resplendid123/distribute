package org.example.agent.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Agent配置管理器
 * 负责管理Agent的动态配置，如心跳间隔等
 * 支持运行时动态更新配置
 */
@Component
public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);

    /**
     * 同步频率（秒），默认30秒
     * 这个值可以被服务端动态修改
     */
    private volatile Integer syncFrequencySeconds = 30;

    /**
     * 心跳间隔（毫秒），由 syncFrequencySeconds * 1000 计算得出
     */
    private volatile Long heartbeatIntervalMs = 30000L;

    /**
     * 获取当前的同步频率（秒）
     *
     * @return 同步频率，单位秒
     */
    public Integer getSyncFrequencySeconds() {
        return syncFrequencySeconds;
    }

    /**
     * 获取当前的心跳间隔（毫秒）
     *
     * @return 心跳间隔，单位毫秒
     */
    public Long getHeartbeatIntervalMs() {
        return heartbeatIntervalMs;
    }

    /**
     * 更新同步频率配置
     * 该方法是线程安全的，使用volatile保证可见性
     *
     * @param syncFrequencySeconds 新的同步频率，单位秒
     */
    public synchronized void updateSyncFrequency(Integer syncFrequencySeconds) {
        if (syncFrequencySeconds == null || syncFrequencySeconds <= 0) {
            log.warn("Invalid sync frequency: {}. Using default 30 seconds", syncFrequencySeconds);
            syncFrequencySeconds = 30;
        }

        if (!this.syncFrequencySeconds.equals(syncFrequencySeconds)) {
            long oldInterval = this.heartbeatIntervalMs;
            this.syncFrequencySeconds = syncFrequencySeconds;
            this.heartbeatIntervalMs = syncFrequencySeconds * 1000L;
            log.info("Sync frequency updated: {} -> {} seconds (heartbeat interval: {} -> {} ms)",
                    oldInterval / 1000, syncFrequencySeconds, oldInterval, this.heartbeatIntervalMs);
        }
    }
}
