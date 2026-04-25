package com.redmoon2333.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.redmoon2333.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
