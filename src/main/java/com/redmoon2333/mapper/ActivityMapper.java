package com.redmoon2333.mapper;

import com.redmoon2333.entity.Activity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ActivityMapper {
    
    @Insert("INSERT INTO activity(activity_name, background, significance, purpose, process, create_time, update_time) " +
            "VALUES(#{activityName}, #{background}, #{significance}, #{purpose}, #{process}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "activityId")
    int insert(Activity activity);
    
    @Select("SELECT * FROM activity WHERE activity_id = #{activityId}")
    @Results({
        @Result(property = "activityId", column = "activity_id"),
        @Result(property = "activityName", column = "activity_name"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time")
    })
    Activity selectById(Integer activityId);
    
    @Select("SELECT * FROM activity ORDER BY create_time DESC")
    @Results({
        @Result(property = "activityId", column = "activity_id"),
        @Result(property = "activityName", column = "activity_name"),
        @Result(property = "createTime", column = "create_time"),
        @Result(property = "updateTime", column = "update_time")
    })
    List<Activity> selectAll();
    
    @Update("UPDATE activity SET activity_name=#{activityName}, background=#{background}, significance=#{significance}, " +
            "purpose=#{purpose}, process=#{process}, update_time=#{updateTime} WHERE activity_id=#{activityId}")
    int update(Activity activity);
    
    @Delete("DELETE FROM activity WHERE activity_id = #{activityId}")
    int deleteById(Integer activityId);
}