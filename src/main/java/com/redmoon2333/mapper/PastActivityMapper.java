package com.redmoon2333.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.redmoon2333.entity.PastActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PastActivityMapper extends BaseMapper<PastActivity> {

    IPage<PastActivity> findAll(Page<PastActivity> page);

    IPage<PastActivity> findByYear(Page<PastActivity> page, @Param("year") Integer year);

    IPage<PastActivity> findByTitleLike(Page<PastActivity> page, @Param("title") String title);

    IPage<PastActivity> findByYearAndTitleLike(Page<PastActivity> page, @Param("year") Integer year, @Param("title") String title);

    List<Integer> findDistinctYears();

    int countByYear(@Param("year") Integer year);
}
