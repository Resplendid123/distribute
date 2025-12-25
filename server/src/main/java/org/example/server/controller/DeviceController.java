package org.example.server.controller;

import org.example.common.context.Result;
import org.example.server.domain.dto.CommandDto;
import org.example.server.domain.dto.ConfigDto;
import org.example.server.domain.vo.DeviceVo;
import org.example.server.service.DeviceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * 发送命令给设备
     */
    @PostMapping("/command")
    public ResponseEntity<Result<Void>> sendCommand(@RequestBody CommandDto commandDto) {
        Result<Void> result = deviceService.sendCommand(commandDto);
        return ResponseEntity.ok(result);
    }

    /**
     * 更新设备配置
     */
    @PostMapping("/config")
    public ResponseEntity<Result<Void>> updateConfig(@RequestBody ConfigDto configDto) {
        Result<Void> result = deviceService.updateConfig(configDto);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取设备列表
     */
    @GetMapping("/list")
    public ResponseEntity<Result<List<DeviceVo>>> getDeviceList() {
        Result<List<DeviceVo>> result = deviceService.getDeviceList();
        return ResponseEntity.ok(result);
    }

    /**
     * 导出报表
     */
    @GetMapping("/export")
    public void exportReport(HttpServletResponse response) {
        deviceService.exportExcel(response);
    }
}
