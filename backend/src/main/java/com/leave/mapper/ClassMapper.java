package com.leave.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leave.entity.Clazz;
import com.leave.vo.ClassRowVO;
import org.apache.ibatis.annotations.Param;

public interface ClassMapper extends BaseMapper<Clazz> {

    IPage<ClassRowVO> selectClassPage(Page<ClassRowVO> page,
                                      @Param("keyword") String keyword,
                                      @Param("collegeId") Long collegeId);
}
