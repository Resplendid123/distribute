package org.example.socket.service;

import org.example.socket.domain.Device;
import org.example.socket.mapper.DeviceMapper;
import org.example.common.constant.DeviceStatusConstant;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

/**
 * 设备管理服务
 * 负责在数据库中创建、更新设备记录
 */
@Service
public class DeviceManagementService {

    private static final Logger log = LoggerFactory.getLogger(DeviceManagementService.class);
    private final DeviceMapper deviceMapper;
    private final Integer defaultSyncFrequency;

    public DeviceManagementService(DeviceMapper deviceMapper,
                                   @Value("${device.default-sync-frequency:30}") Integer defaultSyncFrequency) {
        this.deviceMapper = deviceMapper;
        this.defaultSyncFrequency = defaultSyncFrequency;
    }

    /**
     * 注册或更新设备
     * Agent连接时调用，按设备名称(name)查询是否已存在
     * - 如果存在，更新状态为ONLINE并返回原ID
     * - 如果不存在，创建新的设备记录
     * 
     * @param name 设备名称(Agent name)
     * @param ipAddress 代理IP地址
     * @return 设备ID（复用旧ID或新创建的ID）
     */
    public Long registerOrUpdateDevice(String name, String ipAddress) {
        try {
            // 按name查询是否已存在该设备
            QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", name);
            Device existingDevice = deviceMapper.selectOne(queryWrapper);
            
            if (existingDevice != null) {
                // 设备已存在，更新状态为ONLINE
                existingDevice.setStatusCode(DeviceStatusConstant.ONLINE);
                existingDevice.setLastHeartbeatAt(LocalDateTime.now());
                existingDevice.setUpdatedAt(LocalDateTime.now());
                if (ipAddress != null) {
                    existingDevice.setIpAddress(ipAddress);
                }
                deviceMapper.updateById(existingDevice);
                log.info("Device updated (reused ID): id={}, name={}, ipAddress={}", existingDevice.getId(), name, ipAddress);
                return existingDevice.getId();
            } else {
                // 设备不存在，创建新记录
                Device newDevice = new Device();
                newDevice.setName(name);
                newDevice.setIpAddress(ipAddress);
                newDevice.setStatusCode(DeviceStatusConstant.ONLINE);
                newDevice.setSyncFrequency(defaultSyncFrequency);
                newDevice.setCreatedAt(LocalDateTime.now());
                newDevice.setUpdatedAt(LocalDateTime.now());
                newDevice.setLastHeartbeatAt(LocalDateTime.now());
                
                deviceMapper.insert(newDevice);
                log.info("Device created: id={}, name={}, ipAddress={}", newDevice.getId(), name, ipAddress);
                return newDevice.getId();
            }
        } catch (Exception e) {
            log.error("Error registering device: {}", name, e);
            return null;
        }
    }

    /**
     * 标记设备离线
     * Agent断开连接时调用
     * 
     * @param deviceId 设备ID
     */
    public void markDeviceOffline(Long deviceId) {
        try {
            Device device = deviceMapper.selectById(deviceId);
            
            if (device != null) {
                device.setStatusCode(DeviceStatusConstant.OFFLINE);
                device.setUpdatedAt(LocalDateTime.now());
                deviceMapper.updateById(device);
                log.info("Device marked offline: id={}", deviceId);
            } else {
                log.warn("Device not found for offline marking: id={}", deviceId);
            }
        } catch (Exception e) {
            log.error("Error marking device offline: {}", deviceId, e);
        }
    }

    /**
     * 更新设备心跳时间
     * 
     * @param deviceId 设备ID
     */
    public void updateDeviceHeartbeat(Long deviceId) {
        try {
            Device device = deviceMapper.selectById(deviceId);
            
            if (device != null) {
                LocalDateTime now = LocalDateTime.now();
                device.setLastHeartbeatAt(now);
                device.setStatusCode(DeviceStatusConstant.ONLINE);
                device.setUpdatedAt(now);
                deviceMapper.updateById(device);
                log.debug("Device heartbeat updated: id={}, lastHeartbeatAt={}", deviceId, now);
            } else {
                log.warn("Device not found for heartbeat update: id={}", deviceId);
            }
        } catch (Exception e) {
            log.error("Error updating device heartbeat: {}", deviceId, e);
        }
    }

    /**
     * 更新设备状态信息
     * 
     * @param deviceId 设备ID
     * @param statusNode JSON状态数据
     */
    public void updateDeviceStatus(Long deviceId, JsonNode statusNode) {
        try {
            Device device = deviceMapper.selectById(deviceId);
            
            if (device != null && statusNode != null) {
                device.setInfo(statusNode.toString());
                device.setUpdatedAt(LocalDateTime.now());
                deviceMapper.updateById(device);
                log.debug("Device status updated: id={}", deviceId);
            } else {
                log.warn("Device not found for status update: id={}", deviceId);
            }
        } catch (Exception e) {
            log.error("Error updating device status: {}", deviceId, e);
        }
    }

    /**
     * 根据设备ID获取设备信息
     * 
     * @param deviceId 设备ID
     * @return 设备对象，如果不存在则返回null
     */
    public Device getDeviceById(Long deviceId) {
        try {
            return deviceMapper.selectById(deviceId);
        } catch (Exception e) {
            log.error("Error getting device by id: {}", deviceId, e);
            return null;
        }
    }
}
