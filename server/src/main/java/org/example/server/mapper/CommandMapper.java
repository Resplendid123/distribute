package org.example.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.server.domain.pojo.Command;

@Mapper
public interface CommandMapper extends BaseMapper<Command> {

}
