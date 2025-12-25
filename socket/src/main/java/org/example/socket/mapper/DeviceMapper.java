package org.example.socket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.socket.domain.Device;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
}
