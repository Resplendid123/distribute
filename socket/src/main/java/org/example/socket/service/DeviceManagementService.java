package org.example.socket.service;

import org.example.socket.domain.Device;
import org.example.socket.mapper.DeviceMapper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.time.LocalDateTime;

/**
 * 设备管理服务
 * 负责在数据库中创建、更新设备记录
 * 
 * 关键概念：
 * - agentId: 字符串标识，对应数据库Device表的name字段
 * - deviceId: 数据库主键ID（Long类型）
 */
@Service
public class DeviceManagementService {

    private static final Logger log = LoggerFactory.getLogger(DeviceManagementService.class);
    private final DeviceMapper deviceMapper;

    public DeviceManagementService(DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    /**
     * 注册或更新设备
     * Agent连接时调用，通过agentId查询或创建设备记录
     * 
     * @param agentId 代理ID
     * @param ipAddress 代理IP地址
     */
    public void registerOrUpdateDevice(String agentId, String ipAddress) {
        try {
            // 通过 agentId 查询设备
            QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", agentId);
            Device device = deviceMapper.selectOne(queryWrapper);
            
            if (device == null) {
                // 新设备，创建记录
                device = new Device();
                device.setName(agentId);
                device.setIpAddress(ipAddress);
                device.setStatusCode(1); // 1表示在线
                device.setSyncFrequency(30); // 默认30秒
                device.setLastHeartbeatAt(LocalDateTime.now());
                device.setCreatedAt(LocalDateTime.now());
                device.setUpdatedAt(LocalDateTime.now());
                deviceMapper.insert(device);
                log.info("Device created: agentId={}, ipAddress={}", agentId, ipAddress);
            } else {
                // 更新现有设备
                device.setStatusCode(1); // 标记为在线
                device.setLastHeartbeatAt(LocalDateTime.now());
                device.setUpdatedAt(LocalDateTime.now());
                if (ipAddress != null) {
                    device.setIpAddress(ipAddress);
                }
                deviceMapper.updateById(device);
                log.info("Device updated: agentId={}", agentId);
            }
        } catch (Exception e) {
            log.error("Error registering/updating device: {}", agentId, e);
        }
    }

    /**
     * 标记设备离线
     * Agent断开连接时调用
     * 
     * @param agentId 代理ID
     */
    public void markDeviceOffline(String agentId) {
        try {
            QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", agentId);
            Device device = deviceMapper.selectOne(queryWrapper);
            
            if (device != null) {
                device.setStatusCode(0); // 0表示离线
                device.setUpdatedAt(LocalDateTime.now());
                deviceMapper.updateById(device);
                log.info("Device marked offline: {}", agentId);
            } else {
                log.warn("Device not found for offline marking: {}", agentId);
            }
        } catch (Exception e) {
            log.error("Error marking device offline: {}", agentId, e);
        }
    }

    /**
     * 更新设备心跳时间
     * 
     * @param agentId 代理ID
     */
    public void updateDeviceHeartbeat(String agentId) {
        try {
            QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", agentId);
            Device device = deviceMapper.selectOne(queryWrapper);
            
            if (device != null) {
                LocalDateTime now = LocalDateTime.now();
                device.setLastHeartbeatAt(now);
                device.setStatusCode(1); // 确保状态为在线
                device.setUpdatedAt(now);
                deviceMapper.updateById(device);
                log.debug("Device heartbeat updated: agentId={}, lastHeartbeatAt={}", agentId, now);
            } else {
                log.warn("Device not found for heartbeat update: {}", agentId);
            }
        } catch (Exception e) {
            log.error("Error updating device heartbeat: {}", agentId, e);
        }
    }

    /**
     * 更新设备状态信息
     * 
     * @param agentId 代理ID
     * @param statusNode JSON状态数据
     */
    public void updateDeviceStatus(String agentId, JsonNode statusNode) {
        try {
            QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", agentId);
            Device device = deviceMapper.selectOne(queryWrapper);
            
            if (device != null && statusNode != null) {
                device.setInfo(statusNode.toString());
                device.setUpdatedAt(LocalDateTime.now());
                deviceMapper.updateById(device);
                log.debug("Device status updated: {}", agentId);
            } else {
                log.warn("Device not found for status update: {}", agentId);
            }
        } catch (Exception e) {
            log.error("Error updating device status: {}", agentId, e);
        }
    }
}
