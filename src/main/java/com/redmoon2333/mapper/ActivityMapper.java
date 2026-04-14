package com.redmoon2333.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.redmoon2333.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {
    
}