package org.example.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.common.context.Result;
import org.example.server.domain.dto.CommandDto;
import org.example.server.domain.dto.ConfigDto;
import org.example.server.domain.pojo.Device;
import org.example.server.domain.vo.DeviceVo;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

public interface DeviceService extends IService<Device> {

    /**
     * 发送命令给设备
     */
    Result<Void> sendCommand(CommandDto commandDto);

    /**
     * 更新设备配置
     */
    Result<Void> updateConfig(ConfigDto configDto);

    /**
     * 获取设备列表
     */
    Result<List<DeviceVo>> getDeviceList();

    /**
     * 导出Excel报表
     */
    void exportExcel(HttpServletResponse response);
}
