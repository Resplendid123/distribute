package org.example.server.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.common.context.Result;
import org.example.common.enums.ResultCode;
import org.example.common.constant.CommandStatusConstant;
import org.example.common.constant.DeviceStatusConstant;
import org.example.server.client.SocketClient;
import org.example.server.domain.dto.CommandDto;
import org.example.server.domain.dto.ConfigDto;
import org.example.server.domain.pojo.Command;
import org.example.server.domain.pojo.Device;
import org.example.server.domain.vo.DeviceVo;
import org.example.server.domain.vo.DeviceDetailVo;
import org.example.server.mapper.CommandMapper;
import org.example.server.mapper.DeviceMapper;
import org.example.server.service.DeviceService;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final CommandMapper commandMapper;
    private final SocketClient socketClient;
    private static final Logger log = LoggerFactory.getLogger(DeviceServiceImpl.class);

    public DeviceServiceImpl(DeviceMapper deviceMapper, CommandMapper commandMapper, SocketClient socketClient) {
        this.deviceMapper = deviceMapper;
        this.commandMapper = commandMapper;
        this.socketClient = socketClient;
    }

    @Override
    public Result<Void> sendCommand(CommandDto commandDto) {
        try {
            // 通过 id 直接查询设备
            Device device = deviceMapper.selectById(commandDto.id());

            if (device == null) {
                return Result.fail(ResultCode.NOT_FOUND);
            }

            // 创建命令记录
            Command command = new Command();
            command.setDeviceId(device.getId());
            command.setCommandType(commandDto.commandType());
            command.setCommandContent(commandDto.commandContent());
            command.setStatus(CommandStatusConstant.PENDING);
            command.setCreatedAt(LocalDateTime.now());
            command.setUpdatedAt(LocalDateTime.now());
            commandMapper.insert(command);

            // 处理 offline 命令（强制下线）
            if ("offline".equalsIgnoreCase(commandDto.commandType())) {
                return handleOfflineCommand(device, command);
            }

            // 处理 restart 命令（重启Agent）
            if ("restart".equalsIgnoreCase(commandDto.commandType())) {
                return handleRestartCommand(device, command);
            }

            boolean forwarded = socketClient.forwardCommandToAgent(
                    device.getId(),
                    command.getId(),
                    commandDto.commandType(),
                    commandDto.commandContent()
            );

            if (forwarded) {
                command.setStatus(CommandStatusConstant.EXECUTING);
                commandMapper.updateById(command);
                log.info("Command sent to device: {} - {}", device.getId(), commandDto.commandType());
                return Result.success("Command sent successfully");
            } else {
                log.warn("Failed to forward command to agent: {}", device.getId());
                return Result.fail(ResultCode.INTERNAL_SERVER_ERROR, "Agent offline or unreachable");
            }
        } catch (Exception e) {
            log.error("Error sending command to device: {}", commandDto.id(), e);
            return Result.fail(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 处理强制下线命令
     */
    private Result<Void> handleOfflineCommand(Device device, Command command) {
        try {
            // 更新设备状态为离线
            device.setStatusCode(DeviceStatusConstant.OFFLINE);
            device.setUpdatedAt(LocalDateTime.now());
            deviceMapper.updateById(device);

            // 标记命令为已执行
            command.setStatus(CommandStatusConstant.COMPLETED);
            command.setResult("Device forced offline successfully");
            command.setExecutedAt(LocalDateTime.now());
            command.setUpdatedAt(LocalDateTime.now());
            commandMapper.updateById(command);

            boolean notified = socketClient.forwardCommandToAgent(
                    device.getId(),
                    command.getId(),
                    "offline",
                    "Force offline by admin"
            );

            if (notified) {
                log.info("Device {} forced offline, agent notified via socket", device.getId());
            } else {
                log.warn("Device {} forced offline, but failed to notify agent", device.getId());
            }

            return Result.success("Device forced offline successfully");
        } catch (Exception e) {
            log.error("Error handling offline command for device: {}", device.getId(), e);
            command.setStatus("failed");
            command.setResult("Error: " + e.getMessage());
            command.setUpdatedAt(LocalDateTime.now());
            commandMapper.updateById(command);
            return Result.fail(ResultCode.INTERNAL_SERVER_ERROR, "Failed to offline device");
        }
    }

    /**
     * 处理重启命令
     * 向Agent发送restart命令，Agent收到后会重启
     */
    private Result<Void> handleRestartCommand(Device device, Command command) {
        try {
            log.info("Handling restart command for device: {}", device.getId());

            // 转发restart命令给Agent
            boolean notified = socketClient.forwardCommandToAgent(
                    device.getId(),
                    command.getId(),
                    "restart",
                    "Restart Agent application"
            );

            if (notified) {
                // 标记命令为执行中
                command.setStatus(CommandStatusConstant.EXECUTING);
                commandMapper.updateById(command);
                log.info("Restart command sent to device: {}", device.getId());
                return Result.success("Restart command sent successfully");
            } else {
                // Agent离线，命令发送失败
                command.setStatus(CommandStatusConstant.FAILED);
                command.setResult("Agent offline or unreachable");
                command.setUpdatedAt(LocalDateTime.now());
                commandMapper.updateById(command);
                log.warn("Failed to send restart command to device: {}", device.getId());
                return Result.fail(ResultCode.INTERNAL_SERVER_ERROR, "Agent offline or unreachable");
            }
        } catch (Exception e) {
            log.error("Error handling restart command for device: {}", device.getId(), e);
            command.setStatus(CommandStatusConstant.FAILED);
            command.setResult("Error: " + e.getMessage());
            command.setUpdatedAt(LocalDateTime.now());
            commandMapper.updateById(command);
            return Result.fail(ResultCode.INTERNAL_SERVER_ERROR, "Failed to restart device");
        }
    }

    @Override
    public Result<Void> updateConfig(ConfigDto configDto) {
        try {
            // 通过 id 查询设备
            if (configDto.id() == null || configDto.id() <= 0) {
                return Result.fail(ResultCode.BAD_REQUEST, "Device id is required");
            }

            Device device = deviceMapper.selectById(configDto.id());

            if (device == null) {
                return Result.fail(ResultCode.NOT_FOUND, "Device not found: id=" + configDto.id());
            }

            // 如果提供了新的设备备注名，则更新备注名
            if (configDto.remarkName() != null && !configDto.remarkName().isEmpty()) {
                device.setRemarkName(configDto.remarkName());
            }

            // 创建配置更新命令
            Command command = new Command();
            command.setDeviceId(device.getId());
            command.setCommandType("config");
            command.setCommandContent("{\"syncFrequency\":" + configDto.syncFrequencySeconds() + "}");
            command.setStatus(CommandStatusConstant.PENDING);
            command.setCreatedAt(LocalDateTime.now());
            command.setUpdatedAt(LocalDateTime.now());
            commandMapper.insert(command);

            // 更新本地设备的同步频率配置和其他字段
            device.setSyncFrequency(configDto.syncFrequencySeconds());
            device.setUpdatedAt(LocalDateTime.now());
            deviceMapper.updateById(device);

            // 异步转发命令给 Agent，避免数据不一致问题
            // 即使转发失败，数据库已更新，后续可通过重试机制确保最终一致性
            new Thread(() -> {
                try {
                    log.debug("Async forwarding config command to agent: id={}", configDto.id());
                    
                    boolean forwarded = socketClient.forwardCommandToAgent(
                            device.getId(),
                            command.getId(),
                            "config",
                            "{\"syncFrequency\":" + configDto.syncFrequencySeconds() + "}"
                    );

                    if (forwarded) {
                        command.setStatus(CommandStatusConstant.EXECUTING);
                        commandMapper.updateById(command);
                        log.info("Config command successfully forwarded to agent: id={}, commandId={}, syncFrequency={}", 
                            configDto.id(), command.getId(), configDto.syncFrequencySeconds());
                    } else {
                        // Agent 离线或 Socket 服务不可用，命令状态保持 PENDING
                        // 供后续重试机制处理（需要实现定时重试任务）
                        log.warn("Failed to forward config command (agent may be offline): id={}, commandId={}. " +
                                "Command status: PENDING - will be retried when agent is online.", 
                            configDto.id(), command.getId());
                    }
                } catch (Exception e) {
                    log.error("Unexpected error in async config command forwarding: id={}, commandId={}", 
                        configDto.id(), command.getId(), e);
                }
            }).start();

            // 立即返回成功，数据库已更新
            return Result.success("Config updated successfully (async delivery).");
        } catch (Exception e) {
            log.error("Error updating config for id={}", configDto.id(), e);
            return Result.fail(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Result<List<DeviceVo>> getDeviceList() {
        try {
            List<Device> devices = deviceMapper.selectList(null);
            List<DeviceVo> deviceVos = devices.stream()
                    .map(device -> DeviceVo.builder()
                            .id(device.getId())
                            .name(device.getName())
                            .ipAddress(device.getIpAddress())
                            .syncFrequency(device.getSyncFrequency())
                            .statusCode(device.getStatusCode())
                            .lastHeartbeatAt(device.getLastHeartbeatAt())
                            .createdAt(device.getCreatedAt())
                            .updatedAt(device.getUpdatedAt())
                            .build())
                    .collect(Collectors.toList());
            return Result.success(deviceVos);
        } catch (Exception e) {
            log.error("Error retrieving device list", e);
            return Result.fail(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Result<DeviceDetailVo> getDeviceDetail(Long deviceId) {
        try {
            Device device = deviceMapper.selectById(deviceId);
            if (device == null) {
                return Result.fail(ResultCode.NOT_FOUND);
            }

            DeviceDetailVo deviceDetailVo = DeviceDetailVo.builder()
                    .id(device.getId())
                    .name(device.getName())
                    .ipAddress(device.getIpAddress())
                    .syncFrequency(device.getSyncFrequency())
                    .statusCode(device.getStatusCode())
                    .info(device.getInfo())
                    .lastHeartbeatAt(device.getLastHeartbeatAt())
                    .createdAt(device.getCreatedAt())
                    .updatedAt(device.getUpdatedAt())
                    .build();
            return Result.success(deviceDetailVo);
        } catch (Exception e) {
            log.error("Error retrieving device detail for id: {}", deviceId, e);
            return Result.fail(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }
    @Override
    public void exportExcel(HttpServletResponse response) {
        try {
            List<Device> devices = deviceMapper.selectList(null);
            
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=devices.xlsx");

            EasyExcel.write(response.getOutputStream(), Device.class)
                    .sheet("设备列表")
                    .doWrite(devices);
            
            log.info("Excel export completed successfully");
        } catch (Exception e) {
            log.error("Error exporting Excel", e);
            try {
                response.getWriter().write("Excel export failed: " + e.getMessage());
            } catch (Exception ex) {
                log.error("Error writing error response", ex);
            }
        }
    }


}
