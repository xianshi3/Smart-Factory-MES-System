package com.mes.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mes.common.entity.SysUserAuth;
import org.apache.ibatis.annotations.Mapper;

/**
 * sys_user 只读 Mapper（权限校验用）
 */
@Mapper
public interface SysUserAuthMapper extends BaseMapper<SysUserAuth> {
}