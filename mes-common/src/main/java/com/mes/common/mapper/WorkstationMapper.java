package com.mes.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.common.entity.Workstation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 工位 Mapper 接口
 * @author MES
 * @since 2024
 */
@Mapper
public interface WorkstationMapper extends BaseMapper<Workstation> {

    /**
     * 行锁：锁定指定工位，用于排产类写操作的串行化（同一工位上的排产变更互斥）。
     */
    @Select("SELECT * FROM mes_workstation WHERE id = #{id} FOR UPDATE")
    Workstation selectByIdForUpdate(@Param("id") Long id);
}