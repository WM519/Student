package com.leave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leave.entity.College;
import com.leave.vo.CollegeRowVO;
import org.apache.ibatis.annotations.Param;

public interface CollegeMapper extends BaseMapper<College> {

    IPage<CollegeRowVO> selectCollegePage(Page<CollegeRowVO> page, @Param("keyword") String keyword);
}
