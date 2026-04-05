package com.mes.common.result;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果类
 * 用于分页查询的标准化响应格式
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {
    /** 总记录数 */
    private long total;
    /** 数据列表 */
    private List<T> records;
    /** 当前页码 */
    private long current;
    /** 每页大小 */
    private long size;
    /** 总页数 */
    private long pages;

    /**
     * 从参数构造分页结果
     */
    public static <T> PageResult<T> of(long total, List<T> records, long current, long size) {
        long pages = (total + size - 1) / size;
        return new PageResult<>(total, records, current, size, pages);
    }

    /**
     * 从MyBatis-Plus Page对象构造分页结果
     */
    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page.getTotal(), page.getRecords(), page.getCurrent(), page.getSize(), page.getPages());
    }
}
