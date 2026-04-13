package com.mes.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.common.entity.Menu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜单Mapper接口
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {
}