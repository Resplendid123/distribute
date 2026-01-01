package org.example.socket.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.common.context.Result;
import org.example.common.enums.ResultCode;
import org.example.socket.domain.Device;
import org.example.socket.mapper.DeviceMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Socket 模块设备控制器
 * 提供设备相关的 API 接口
 */
@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private static final Logger log = LoggerFactory.getLogger(DeviceController.class);
    private final DeviceMapper deviceMapper;

    public DeviceController(DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    /**
     * 根据设备 ID 获取同步频率
     *
     * @param id 设备 ID
     * @return 同步频率（秒）
     */
    @GetMapping("/sync-frequency/{id}")
    public ResponseEntity<Result<Integer>> getSyncFrequency(@PathVariable Long id) {
        try {
            Device device = deviceMapper.selectById(id);
            if (device == null) {
                return ResponseEntity.ok(Result.fail(ResultCode.NOT_FOUND));
            }
            return ResponseEntity.ok(Result.success(device.getSyncFrequency()));
        } catch (Exception e) {
            log.error("Error fetching sync frequency for device id: {}", id, e);
            return ResponseEntity.ok(Result.fail(ResultCode.INTERNAL_SERVER_ERROR));
        }
    }

    /**
     * 根据设备名称获取同步频率
     * Agent 在上线时调用此接口获取同步频率配置
     *
     * @param name 设备名称
     * @return 同步频率（秒）
     */
    @GetMapping("/sync-frequency-by-name/{name}")
    public ResponseEntity<Result<Integer>> getSyncFrequencyByName(@PathVariable String name) {
        try {
            QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", name);
            Device device = deviceMapper.selectOne(queryWrapper);

            if (device == null) {
                return ResponseEntity.ok(Result.fail(ResultCode.NOT_FOUND));
            }
            return ResponseEntity.ok(Result.success(device.getSyncFrequency()));
        } catch (Exception e) {
            log.error("Error fetching sync frequency for device name: {}", name, e);
            return ResponseEntity.ok(Result.fail(ResultCode.INTERNAL_SERVER_ERROR));
        }
    }
}
