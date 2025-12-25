package org.example.server.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.common.context.Result;
import org.example.common.enums.ResultCode;
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
            QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", commandDto.agentId());
            Device device = deviceMapper.selectOne(queryWrapper);

            if (device == null) {
                return Result.fail(ResultCode.NOT_FOUND);
            }

            // 创建命令记录
            Command command = new Command();
            command.setDeviceId(device.getId());
            command.setCommandType(commandDto.commandType());
            command.setCommandContent(commandDto.commandContent());
            command.setStatus("pending");
            command.setCreatedAt(LocalDateTime.now());
            command.setUpdatedAt(LocalDateTime.now());
            commandMapper.insert(command);

            // 处理 offline 命令（强制下线）
            if ("offline".equalsIgnoreCase(commandDto.commandType())) {
                return handleOfflineCommand(device, command);
            }

            boolean forwarded = socketClient.forwardCommandToAgent(
                    commandDto.agentId(),
                    command.getId(),
                    commandDto.commandType(),
                    commandDto.commandContent()
            );

            if (forwarded) {
                command.setStatus("executing");
                commandMapper.updateById(command);
                log.info("Command sent to device: {} - {}", commandDto.agentId(), commandDto.commandType());
                return Result.success("Command sent successfully");
            } else {
                log.warn("Failed to forward command to agent: {}", commandDto.agentId());
                return Result.fail(ResultCode.INTERNAL_SERVER_ERROR, "Agent offline or unreachable");
            }
        } catch (Exception e) {
            log.error("Error sending command to {}", commandDto.agentId(), e);
            return Result.fail(ResultCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 处理强制下线命令
     */
    private Result<Void> handleOfflineCommand(Device device, Command command) {
        try {
            // 更新设备状态为离线
            device.setStatusCode(0); // 0表示离线
            device.setUpdatedAt(LocalDateTime.now());
            deviceMapper.updateById(device);

            // 标记命令为已执行
            command.setStatus("completed");
            command.setResult("Device forced offline successfully");
            command.setExecutedAt(LocalDateTime.now());
            command.setUpdatedAt(LocalDateTime.now());
            commandMapper.updateById(command);

            String agentId = device.getName();
            boolean notified = socketClient.forwardCommandToAgent(
                    agentId,
                    command.getId(),
                    "offline",
                    "Force offline by admin"
            );

            if (notified) {
                log.info("Device {} forced offline, agent notified via socket", agentId);
            } else {
                log.warn("Device {} forced offline, but failed to notify agent", agentId);
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

    @Override
    public Result<Void> updateConfig(ConfigDto configDto) {
        try {
            QueryWrapper<Device> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("ip_address", configDto.ip());
            Device device = deviceMapper.selectOne(queryWrapper);

            if (device == null) {
                return Result.fail(ResultCode.NOT_FOUND);
            }

            // 更新同步频率配置
            device.setSyncFrequency(configDto.syncFrequencySeconds());
            device.setUpdatedAt(LocalDateTime.now());
            deviceMapper.updateById(device);

            log.info("Config updated for device: {} - syncFrequency: {}", configDto.ip(), configDto.syncFrequencySeconds());
            return Result.success("Config updated successfully");
        } catch (Exception e) {
            log.error("Error updating config for {}", configDto.ip(), e);
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
